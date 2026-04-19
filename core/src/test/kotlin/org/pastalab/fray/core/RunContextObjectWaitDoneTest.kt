package org.pastalab.fray.core

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.command.ExecutionInfo
import org.pastalab.fray.core.command.LambdaExecutor
import org.pastalab.fray.core.command.NetworkDelegateType
import org.pastalab.fray.core.command.SystemTimeDelegateType
import org.pastalab.fray.core.concurrency.context.ObjectNotifyContext
import org.pastalab.fray.core.concurrency.operations.InterruptionType
import org.pastalab.fray.core.concurrency.operations.ObjectWakeBlocked
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation
import org.pastalab.fray.core.randomness.ControlledRandomProvider
import org.pastalab.fray.core.scheduler.RandomScheduler
import org.pastalab.fray.rmi.ThreadState

/**
 * Regression for the `ClassCastException: ObjectWakeBlocked cannot be cast to
 * ThreadResumeOperation` thrown from [RunContext.objectWaitDoneImpl].
 *
 * Background: When a thread T is waiting on a JVM monitor and is unblocked (e.g. via notify), the
 * scheduler sets `T.pendingOperation = ObjectWakeBlocked` and `T.state = Runnable`. When the
 * scheduler later picks T to run, it sets `T.state = Running` and then [RunContext.runThread]
 * rewrites the pending operation to [ThreadResumeOperation] and calls notifyAll on the wait object
 * so T's `Object.wait()` actually returns. T then re-enters Fray via `onObjectWaitDone` →
 * [RunContext.objectWaitDoneImpl], which loops until `state == Running` and then unconditionally
 * casts the pending operation to [ThreadResumeOperation].
 *
 * That cast is unsound. The JVM permits spurious wakeups of `Object.wait()` (per the
 * `java.lang.Object#wait` Javadoc), and the Fray scheduler exploits this — `objectWaitImpl`
 * randomly synthesises spurious unblocks. If T's blocking `wait()` returns spuriously after the
 * scheduler has flipped `state = Running` but before [runThread] has rewritten the pending
 * operation (or just on a memory-visibility race in that window), T observes `state == Running`
 * while `pendingOperation` is still [ObjectWakeBlocked], and the cast blows up with a
 * ClassCastException.
 *
 * This was reported in the Tapestry/Crochet stripe-lock memo
 * (`feedback_crochet_fray_stripe_lock_deadlock.md`): the Kafka workload (`KafkaAdminClient.close` →
 * `Thread.join` → `Object.wait`) intermittently crashes Fray internally with this CCE under heavy
 * notify churn.
 *
 * The fix in [RunContext.objectWaitDoneImpl] mirrors what [runThread] would have done: convert any
 * leftover [ObjectWakeBlocked] / [ConditionWakeBlocked] into a [ThreadResumeOperation] before the
 * cast. This test reproduces the exact conditions the race produces — `state = Running`,
 * `pendingOperation = ObjectWakeBlocked` — by setting the fields directly, then asserts that
 * `objectWaitDoneImpl` returns successfully without throwing.
 */
class RunContextObjectWaitDoneTest {
  val context =
      RunContext(
          Configuration(
              ExecutionInfo(
                  LambdaExecutor({}),
                  ignoreUnhandledExceptions = false,
                  interleaveMemoryOps = false,
                  maxScheduledStep = -1,
              ),
              Path.of("/tmp/fray"),
              1,
              1000,
              RandomScheduler(),
              ControlledRandomProvider(),
              fullSchedule = false,
              exploreMode = false,
              noExitWhenBugFound = true,
              isReplay = false,
              noFray = false,
              dummyRun = false,
              networkDelegateType = NetworkDelegateType.PROACTIVE,
              systemTimeDelegateType = SystemTimeDelegateType.MOCK,
              100_000L,
              ignoreTimedBlock = true,
              sleepAsYield = false,
              true,
              false,
              false,
          )
      )

  @BeforeEach
  fun setUp() {
    context.start()
  }

  /**
   * The exact race the bug observes: the thread sees `state == Running` while its
   * `pendingOperation` is still [ObjectWakeBlocked] from a prior unblock. Pre-fix this triggered a
   * ClassCastException at the cast on the last line of [RunContext.objectWaitDoneImpl]; post-fix
   * the function converts to [ThreadResumeOperation] and returns its `noTimeout`.
   */
  @Test
  fun objectWaitDoneSurvivesObjectWakeBlocked() {
    val tid = Thread.currentThread().id
    val threadContext = context.registeredThreads[tid]!!

    val waitObject = Object()
    val signalContext = context.signalManager.getContext(waitObject) as ObjectNotifyContext
    val lockContext = signalContext.lockContext

    // Establish the same lock-manager bookkeeping that a real wait would set up: this thread
    // holds the wait object's monitor lock at the Fray level, since `objectWaitDoneImpl` will
    // attempt to re-acquire it via `lockBecauseOfWait = true` after the loop exits and we want
    // that re-acquire path (and in particular the `wakingThreads` interaction) to stay reachable.
    signalContext.addWaitingThread(threadContext, /* blockedUntil= */ -1L, /* canInterrupt= */ true)
    // Simulate the unblock that put the thread into the WakeBlocked / Runnable transitional
    // state: the ObjectNotifyContext flips the pending operation to ObjectWakeBlocked and
    // promotes the thread back to Runnable when canLock is true.
    signalContext.unblockThread(tid, InterruptionType.RESOURCE_AVAILABLE)
    assertTrue(
        threadContext.pendingOperation is ObjectWakeBlocked,
        "test setup precondition: expected pending op to be ObjectWakeBlocked, got ${threadContext.pendingOperation}",
    )

    // Simulate the race: scheduler flipped state = Running, but `runThread` has not yet
    // converted ObjectWakeBlocked -> ThreadResumeOperation.
    threadContext.state = ThreadState.Running

    // Pre-fix this throws ClassCastException: ObjectWakeBlocked cannot be cast to
    // ThreadResumeOperation.
    val noTimeout = context.objectWaitDoneImpl(waitObject, /* canInterrupt= */ true)

    // RESOURCE_AVAILABLE => noTimeout == true (per
    // ObjectNotifyContext.updatedThreadContextDueToUnblock)
    assertTrue(noTimeout, "RESOURCE_AVAILABLE unblock should report noTimeout=true")
    val finalOp = threadContext.pendingOperation
    assertNotNull(finalOp)
    assertTrue(
        finalOp is ThreadResumeOperation,
        "expected pending op to be promoted to ThreadResumeOperation, got $finalOp",
    )
    // Sanity: lock should now be held by this thread (objectWaitDoneImpl re-acquires via
    // lockBecauseOfWait = true).
    assertTrue(
        lockContext.isLockHolder(tid),
        "expected this thread to hold the lock after objectWaitDone",
    )
  }

  /**
   * Companion case: when the unblock came from a TIMEOUT, `noTimeout` propagates as `false`. This
   * confirms the converted `ThreadResumeOperation` carries through the timeout flag from the
   * original [ObjectWakeBlocked].
   */
  @Test
  fun objectWaitDoneSurvivesObjectWakeBlockedFromTimeout() {
    val tid = Thread.currentThread().id
    val threadContext = context.registeredThreads[tid]!!

    val waitObject = Object()
    val signalContext = context.signalManager.getContext(waitObject) as ObjectNotifyContext
    signalContext.addWaitingThread(threadContext, /* blockedUntil= */ -1L, /* canInterrupt= */ true)
    signalContext.unblockThread(tid, InterruptionType.TIMEOUT)
    threadContext.state = ThreadState.Running

    val noTimeout = context.objectWaitDoneImpl(waitObject, /* canInterrupt= */ true)
    assertFalse(noTimeout, "TIMEOUT unblock should report noTimeout=false")
    assertTrue(threadContext.pendingOperation is ThreadResumeOperation)
  }
}
