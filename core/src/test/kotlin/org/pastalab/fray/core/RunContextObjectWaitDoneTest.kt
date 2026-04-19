package org.pastalab.fray.core

import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.command.ExecutionInfo
import org.pastalab.fray.core.command.LambdaExecutor
import org.pastalab.fray.core.command.NetworkDelegateType
import org.pastalab.fray.core.command.SystemTimeDelegateType
import org.pastalab.fray.core.concurrency.context.ConditionSignalContext
import org.pastalab.fray.core.concurrency.context.ObjectNotifyContext
import org.pastalab.fray.core.concurrency.operations.InterruptionType
import org.pastalab.fray.core.randomness.ControlledRandomProvider
import org.pastalab.fray.core.scheduler.RandomScheduler
import org.pastalab.fray.rmi.ThreadState

/**
 * Regression for the `ClassCastException: ObjectWakeBlocked cannot be cast to
 * ThreadResumeOperation` thrown from [RunContext.objectWaitDoneImpl] (issue #424).
 *
 * Fray assumes its internal scheduler state is mutated sequentially: by the time a waiter's loop in
 * [RunContext.objectWaitDoneImpl] exits with `state == Running`, the scheduler thread's
 * [RunContext.runThread] must have already rewritten `pendingOperation` from `*WakeBlocked` to
 * [org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation]. When something races with
 * Fray's scheduler (a JVMTI agent, a bytecode transformer, or a misbehaving test harness running on
 * a thread Fray does not schedule), the waiter can observe `state == Running` while
 * `pendingOperation` is still `*WakeBlocked`, and the subsequent cast produced a bare
 * `ClassCastException` with no diagnostic context. That CCE misled investigators into treating the
 * symptom as a Fray bug rather than the host-environment race it actually surfaces.
 *
 * This test simulates that invariant violation by directly setting `state = Running` after the
 * scheduler has moved the thread to `*WakeBlocked` / `Runnable` but before `runThread` would
 * ordinarily convert the pending op. The assertion is that [RunContext.objectWaitDoneImpl] now
 * throws [FrayInternalError] with a message naming the observed pending-op type and pointing at the
 * likely causes, instead of the cryptic `ClassCastException` users previously saw.
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
   * Invariant violation with `pendingOperation = ObjectWakeBlocked` while `state = Running`. Must
   * produce a diagnostic [FrayInternalError] that names the observed op type, not a bare
   * `ClassCastException`.
   */
  @Test
  fun objectWaitDoneReportsInvariantViolationOnObjectWakeBlocked() {
    val tid = Thread.currentThread().id
    val threadContext = context.registeredThreads[tid]!!

    val waitObject = Object()
    val signalContext = context.signalContextFor(waitObject) as ObjectNotifyContext
    signalContext.addWaitingThread(threadContext, /* blockedUntil= */ -1L, /* canInterrupt= */ true)
    signalContext.unblockThread(tid, InterruptionType.RESOURCE_AVAILABLE)

    // Simulate an out-of-sequence mutation: scheduler state flipped to Running without the
    // corresponding `runThread` conversion having run.
    threadContext.state = ThreadState.Running

    val err =
        assertFailsWith<FrayInternalError> {
          context.objectWaitDoneImpl(waitObject, /* canInterrupt= */ true)
        }
    assertTrue(
        err.message!!.contains("ObjectWakeBlocked"),
        "diagnostic should name the observed pending-op type, got: ${err.message}",
    )
    assertTrue(
        err.message!!.contains("#424"),
        "diagnostic should point at the tracking issue, got: ${err.message}",
    )
  }

  /**
   * [java.util.concurrent.locks.Condition] analogue of the same invariant violation. Setup goes
   * through the production `lockNewCondition` entry point — the same path a user calling
   * `lock.newCondition()` takes — so the `ConditionSignalContext` / `LockContext` bookkeeping is
   * realistic. Before the diagnostic was added, this path produced `ClassCastException:
   * ConditionWakeBlocked cannot be cast to ThreadResumeOperation`.
   */
  @Test
  fun conditionAwaitDoneReportsInvariantViolationOnConditionWakeBlocked() {
    val tid = Thread.currentThread().id
    val threadContext = context.registeredThreads[tid]!!

    val lock = ReentrantLock()
    val condition = lock.newCondition()
    context.lockNewCondition(condition, lock)

    val signalContext = context.signalContextFor(condition) as ConditionSignalContext
    val lockContext = signalContext.lockContext

    lockContext.lock(threadContext, false, false, false)
    signalContext.addWaitingThread(threadContext, /* blockedUntil= */ -1L, /* canInterrupt= */ true)
    lockContext.unlock(
        threadContext,
        /* unlockBecauseOfWait= */ true,
        /* earlyExit= */ false,
    )
    signalContext.unblockThread(tid, InterruptionType.RESOURCE_AVAILABLE)
    threadContext.state = ThreadState.Running

    val err =
        assertFailsWith<FrayInternalError> {
          context.objectWaitDoneImpl(condition, /* canInterrupt= */ true)
        }
    assertTrue(
        err.message!!.contains("ConditionWakeBlocked"),
        "diagnostic should name the observed pending-op type, got: ${err.message}",
    )
  }
}
