package org.pastalab.fray.core.delegates

import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.LockSupport
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.locks.StampedLock
import org.pastalab.fray.core.RunContext
import org.pastalab.fray.core.concurrency.operations.BLOCKED_OPERATION_NOT_TIMED
import org.pastalab.fray.runtime.Delegate
import org.pastalab.fray.runtime.MemoryOpType
import org.pastalab.fray.runtime.RangerCondition

class RuntimeDelegate(val context: RunContext, val synchronizer: DelegateSynchronizer) :
    Delegate() {

  override fun onMainExit() {
    // When a main thread exits, we do the following checks:
    // 1. Wait for all threads except the ForkJoinPool common pool threads to finish.
    // 2. Terminate the ForkJoinPool common pool threads.
    // 3. Wait for ForkJoinPool common pool threads to finish.
    if (synchronizer.checkEntered()) return
    context.waitForAllThreadsToFinish(false)
    synchronizer.entered.set(false)
    context.terminateForJoinPool()
    if (synchronizer.checkEntered()) return
    context.waitForAllThreadsToFinish(true)
    context.mainExit()
    synchronizer.entered.set(false)
  }

  fun isSystemThread(thread: Thread): Boolean {
    val systemGroup = thread.threadGroup
    return "InnocuousThreadGroup" == systemGroup?.name || thread.name == "JNA Cleaner"
  }

  private fun onThreadSkip(t: Thread, runnable: () -> Unit) {
    if (synchronizer.entered.get()) {
      return
    }
    if (!context.registeredThreads.contains(Thread.currentThread().id)) {
      return
    }
    if (isSystemThread(t)) {
      return
    }

    try {
      synchronizer.entered.set(true)
      runnable()
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onThreadCreateDone(t: Thread) = onThreadSkip(t) { context.threadCreateDone(t) }

  override fun onThreadStart(t: Thread) {
    synchronizer.onSkipScheduling("Thread.start")
    onThreadSkip(t) { context.threadStart(t) }
  }

  override fun onThreadStartDone(t: Thread) {
    onThreadSkip(t) { context.threadStartDone(t) }
    synchronizer.onSkipSchedulingDone("Thread.start")
  }

  override fun onThreadRun() = synchronizer.runInFrayStartNoSkip { context.threadRun() }

  override fun onThreadEnd() = synchronizer.runInFrayStartNoSkip { context.threadCompleted() }

  override fun onThreadGetState(t: Thread, state: Thread.State): Thread.State =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          { context.threadGetState(t, state) },
          { state },
      )

  override fun onObjectWait(o: Any, timeout: Long) =
      synchronizer.runInFrayStartNoSkip {
        val timeout =
            if (timeout == 0L) BLOCKED_OPERATION_NOT_TIMED
            else timeout + context.timeController.currentTimeMillisRawNoIncrement()
        context.objectWait(o, timeout)
      }

  override fun onObjectWaitDone(o: Any) =
      synchronizer.runInFrayDoneNoSkip { context.objectWaitDone(o).map {} }

  override fun onObjectNotify(o: Any) = synchronizer.runInFrayDoneNoSkip { context.objectNotify(o) }

  override fun onObjectNotifyAll(o: Any) =
      synchronizer.runInFrayDoneNoSkip { context.objectNotifyAll(o) }

  override fun onLockLockInterruptibly(l: Lock) =
      synchronizer.runInFrayStart("Lock.lock") { context.lockLock(l, true) }

  override fun onLockHasQueuedThreads(l: Lock, result: Boolean): Boolean =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          { context.lockHasQueuedThreads(l) },
          { result },
      )

  override fun onLockHasQueuedThread(l: Lock, t: Thread, result: Boolean): Boolean =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          { context.lockHasQueuedThread(l, t) },
          { result },
      )

  override fun onLockTryLock(l: Lock) =
      synchronizer.runInFrayStart("Lock.tryLock") {
        context.lockTryLock(l, canInterrupt = false, blockedUntil = BLOCKED_OPERATION_NOT_TIMED)
      }

  override fun onLockTryLockDone(l: Lock) =
      synchronizer.runInFrayDone("Lock.tryLock") { Result.success(Unit) }

  override fun onLockTryLockInterruptibly(l: Lock, timeout: Long, unit: TimeUnit): Long =
      synchronizer.runInFrayStartWithOriginBlock(
          "Lock.tryLock",
          {
            context
                .lockTryLock(
                    l,
                    canInterrupt = true,
                    context.timeController.currentTimeMillisRawNoIncrement() +
                        unit.toMillis(timeout),
                )
                .map { 0 }
          },
          { timeout },
      )

  override fun onLockTryLockInterruptiblyDone(l: Lock) =
      synchronizer.runInFrayDone("Lock.tryLock") { Result.success(Unit) }

  override fun onLockLock(l: Lock) =
      synchronizer.runInFrayStart("Lock.lock") { context.lockLock(l, false) }

  override fun onLockLockDone() = synchronizer.runInFrayDone("Lock.lock") { Result.success(Unit) }

  override fun onAtomicOperation(o: Any, type: MemoryOpType) =
      synchronizer.runInFrayStart("AtomicOperation") { context.atomicOperation(o, type) }

  override fun onAtomicOperationDone() =
      synchronizer.runInFrayDone("AtomicOperation") { Result.success(Unit) }

  override fun onLockUnlock(l: Lock) =
      synchronizer.runInFrayStart("Lock.unlock") { context.lockUnlock(l) }

  override fun onLockUnlockDone(l: Lock) =
      synchronizer.runInFrayDone("Lock.unlock") { context.lockUnlockDone(l) }

  override fun onMonitorEnter(o: Any) =
      synchronizer.runInFrayStartNoSkip { context.monitorEnter(o, false) }

  override fun onMonitorExit(o: Any) = synchronizer.runInFrayStartNoSkip { context.monitorExit(o) }

  override fun onMonitorExitDone(o: Any) =
      synchronizer.runInFrayDoneNoSkip { context.lockUnlockDone(o) }

  override fun onLockNewCondition(c: Condition, l: Lock) {
    if (synchronizer.entered.get()) {
      return
    }
    // Here we want to track condition creation even if in skipped sections.
    synchronizer.entered.set(true)
    try {
      context.lockNewCondition(c, l)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onConditionAwait(o: Condition) =
      synchronizer.runInFrayStart("Condition.await") {
        context.conditionAwait(o, canInterrupt = true, BLOCKED_OPERATION_NOT_TIMED)
      }

  override fun onConditionAwaitUninterruptibly(o: Condition) =
      synchronizer.runInFrayStart("Condition.await") {
        context.conditionAwait(o, canInterrupt = false, BLOCKED_OPERATION_NOT_TIMED)
      }

  override fun onConditionAwaitDone(o: Condition) =
      synchronizer.runInFrayDone("Condition.await") { context.conditionAwaitDone(o, true).map {} }

  override fun onConditionAwaitUninterruptiblyDone(o: Condition) =
      synchronizer.runInFrayDone("Condition.await") { context.conditionAwaitDone(o, false).map {} }

  override fun onConditionSignal(o: Condition) =
      synchronizer.runInFrayStart("Condition.signal") { context.conditionSignal(o) }

  override fun onConditionSignalDone(l: Condition) =
      synchronizer.runInFrayDone("Condition.signal") { Result.success(Unit) }

  override fun onConditionSignalAll(o: Condition) =
      synchronizer.runInFrayStart("Condition.signal") { context.conditionSignalAll(o) }

  override fun onUnsafeReadVolatile(o: Any?, offset: Long) {
    if (o == null) return
    synchronizer.runInFrayStartNoSkip {
      context.unsafeOperation(o, offset, MemoryOpType.MEMORY_READ)
    }
  }

  override fun onUnsafeWriteVolatile(o: Any?, offset: Long) {
    if (o == null) return
    synchronizer.runInFrayStartNoSkip {
      context.unsafeOperation(o, offset, MemoryOpType.MEMORY_WRITE)
    }
  }

  override fun onFieldRead(o: Any?, owner: String, name: String, descriptor: String) {
    if (o == null) return
    synchronizer.runInFrayStartNoSkip {
      context.fieldOperation(o, owner, name, MemoryOpType.MEMORY_READ)
    }
  }

  override fun onFieldWrite(o: Any?, owner: String, name: String, descriptor: String) {
    if (o == null) return
    synchronizer.runInFrayStartNoSkip {
      context.fieldOperation(o, owner, name, MemoryOpType.MEMORY_WRITE)
    }
  }

  override fun onStaticFieldRead(owner: String, name: String, descriptor: String) =
      synchronizer.runInFrayStartNoSkip {
        context.fieldOperation(null, owner, name, MemoryOpType.MEMORY_READ)
      }

  override fun onStaticFieldWrite(owner: String, name: String, descriptor: String) =
      synchronizer.runInFrayStartNoSkip {
        context.fieldOperation(null, owner, name, MemoryOpType.MEMORY_WRITE)
      }

  override fun onExit(status: Int) {
    if (synchronizer.checkEntered()) return
    if (status != 0) {
      context.reportError(RuntimeException("Exit with status $status"))
    }
    synchronizer.entered.set(false)
  }

  override fun onYield() = synchronizer.runInFrayStartNoSkip { context.yield() }

  override fun onSkipScheduling(signature: String) {
    synchronizer.onSkipScheduling(signature)
  }

  override fun onSkipSchedulingDone(signature: String) {
    synchronizer.onSkipSchedulingDone(signature)
  }

  override fun onSkipPrimitive(signature: String) {
    synchronizer.onSkipPrimitive(signature)
  }

  override fun onSkipPrimitiveDone(signature: String) {
    synchronizer.onSkipPrimitiveDone(signature)
  }

  override fun onThreadPark() = synchronizer.runInFrayStart("Thread.park") { context.threadPark() }

  override fun onUnsafeThreadParkTimed(isAbsolute: Boolean, time: Long) {
    if (isAbsolute) {
      onThreadParkUntil(time)
    } else {
      onThreadParkNanosInternal(time != 0L, time)
    }
  }

  override fun onThreadParkDone() =
      synchronizer.runInFrayDone("Thread.park") {
        context.threadParkDone(BLOCKED_OPERATION_NOT_TIMED)
      }

  override fun onThreadUnpark(t: Thread?) {
    if (t == null) return
    synchronizer.runInFrayStart("Thread.unpark") { context.threadUnpark(t) }
  }

  override fun onThreadUnparkDone(t: Thread?) {
    if (t == null) return
    synchronizer.runInFrayDone("Thread.unpark") { context.threadUnparkDone(t) }
  }

  override fun onThreadInterrupt(t: Thread) =
      synchronizer.runInFrayStart("Thread.interrupt") { context.threadInterrupt(t) }

  override fun onThreadInterruptDone(t: Thread) =
      synchronizer.runInFrayDone("Thread.interrupt") { context.threadInterruptDone(t) }

  override fun onThreadClearInterrupt(origin: Boolean, t: Thread): Boolean =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          { context.threadClearInterrupt(t) },
          { origin },
      )

  override fun onReentrantReadWriteLockInit(lock: ReentrantReadWriteLock) =
      synchronizer.runInFrayStartNoSkip { context.reentrantReadWriteLockInit(lock) }

  override fun onSemaphoreInit(sem: Semaphore) =
      synchronizer.runInFrayStartNoSkip { context.semaphoreInit(sem) }

  override fun onSemaphoreTryAcquire(sem: Semaphore, permits: Int) =
      synchronizer.runInFrayStart("Semaphore.acquire") {
        context.semaphoreAcquire(
            sem,
            permits,
            shouldBlock = false,
            canInterrupt = true,
            blockedUntil = BLOCKED_OPERATION_NOT_TIMED,
        )
      }

  override fun onSemaphoreTryAcquirePermitsTimeout(
      sem: Semaphore,
      permits: Int,
      timeout: Long,
      unit: TimeUnit
  ): Long =
      synchronizer.runInFrayStartWithOriginBlock(
          "Semaphore.acquire",
          {
            context
                .semaphoreAcquire(
                    sem,
                    permits,
                    shouldBlock = true,
                    canInterrupt = true,
                    blockedUntil =
                        unit.toMillis(timeout) +
                            context.timeController.currentTimeMillisRawNoIncrement(),
                )
                .map { 0L }
          },
          {
            return@runInFrayStartWithOriginBlock timeout
          },
      )

  override fun onSemaphoreAcquire(sem: Semaphore, permits: Int) =
      synchronizer.runInFrayStart("Semaphore.acquire") {
        context.semaphoreAcquire(
            sem,
            permits,
            shouldBlock = true,
            canInterrupt = true,
            BLOCKED_OPERATION_NOT_TIMED,
        )
      }

  override fun onSemaphoreAcquireUninterruptibly(sem: Semaphore, permits: Int) =
      synchronizer.runInFrayStart("Semaphore.acquire") {
        context.semaphoreAcquire(
            sem,
            permits,
            shouldBlock = true,
            canInterrupt = false,
            BLOCKED_OPERATION_NOT_TIMED,
        )
      }

  override fun onSemaphoreAcquireDone() =
      synchronizer.runInFrayDone("Semaphore.acquire") { Result.success(Unit) }

  override fun onSemaphoreRelease(sem: Semaphore, permits: Int) =
      synchronizer.runInFrayStart("Semaphore.release") { context.semaphoreRelease(sem, permits) }

  override fun onSemaphoreReleaseDone() =
      synchronizer.runInFrayDone("Semaphore.release") { Result.success(Unit) }

  override fun onSemaphoreDrainPermits(sem: Semaphore) =
      synchronizer.runInFrayStart("Semaphore.drainPermits") {
        context.semaphoreDrainPermits(sem).map {}
      }

  override fun onSemaphoreDrainPermitsDone() =
      synchronizer.runInFrayDone("Semaphore.drainPermits") { Result.success(Unit) }

  override fun onSemaphoreReducePermits(sem: Semaphore, permits: Int) =
      synchronizer.runInFrayStart("Semaphore.reducePermits") {
        context.semaphoreReducePermits(sem, permits)
      }

  override fun onSemaphoreReducePermitsDone() =
      synchronizer.runInFrayDone("Semaphore.reducePermits") { Result.success(Unit) }

  override fun onLatchAwait(latch: CountDownLatch) =
      synchronizer.runInFrayStart("Latch.await") {
        context.latchAwait(
            latch,
            BLOCKED_OPERATION_NOT_TIMED,
        )
      }

  // Unlike `onLatchAwait`, which is only instrumented at the start and end of the method,
  // `onLatchAwaitTimeout` replaces the original `CountDownLatch.await` method completely because
  // The underlying method call is not deterministic due to the timeout. Therefore, we need to
  // implement a deterministic version of the method.
  override fun onLatchAwaitTimeout(latch: CountDownLatch, timeout: Long, unit: TimeUnit): Boolean {
    runCatching {
      synchronizer.runInFrayStart("Latch.await") {
        context.latchAwait(
            latch,
            context.timeController.currentTimeMillisRawNoIncrement() + unit.toMillis(timeout),
        )
      }
    }
    return synchronizer.runInFrayDoneWithOriginBlock(
        "Latch.await",
        { context.latchAwaitDone(latch) },
        { latch.await(timeout, unit) },
    )
  }

  override fun onLatchAwaitDone(latch: CountDownLatch) =
      synchronizer.runInFrayDone("Latch.await") { context.latchAwaitDone(latch).map {} }

  override fun onLatchCountDown(latch: CountDownLatch) =
      synchronizer.runInFrayStart("Latch.countDown") { context.latchCountDown(latch) }

  override fun onLatchCountDownDone(latch: CountDownLatch) =
      synchronizer.runInFrayDone("Latch.countDown") { context.latchCountDownDone(latch) }

  override fun onReportError(e: Throwable) {
    val originEntered = synchronizer.entered.get()
    synchronizer.entered.set(true)
    context.reportError(e)
    synchronizer.entered.set(originEntered)
  }

  override fun onArrayLoad(o: Any?, index: Int) {
    if (o == null) return
    synchronizer.runInFrayStartNoSkip { context.arrayOperation(o, index, MemoryOpType.MEMORY_READ) }
  }

  override fun onArrayStore(o: Any?, index: Int) {
    if (o == null) return
    synchronizer.runInFrayStartNoSkip {
      context.arrayOperation(o, index, MemoryOpType.MEMORY_WRITE)
    }
  }

  override fun start() {
    // The first thread is not registered.
    // Therefor we cannot call `synchronizer.checkEntered` here.
    synchronizer.entered.set(true)
    try {
      context.start()
    } finally {
      synchronizer.entered.set(false)
    }
  }

  private fun onThreadParkTimed(blockedUntil: Long, originBlock: () -> Unit) {
    synchronizer.runInFrayStart("Thread.park") { context.threadPark() }
    synchronizer.runInFrayDoneWithOriginBlock(
        "Thread.park",
        {
          runCatching { LockSupport.park() }
          context.threadParkDone(blockedUntil)
        },
        originBlock,
    )
  }

  override fun onThreadParkNanos(nanos: Long) = onThreadParkNanosInternal(true, nanos)

  private fun onThreadParkNanosInternal(timed: Boolean, nanos: Long) {
    val blockedUntil =
        if (timed) {
          context.timeController.currentTimeMillisRawNoIncrement() + nanos / 1_000_000
        } else {
          BLOCKED_OPERATION_NOT_TIMED
        }
    onThreadParkTimed(blockedUntil) { LockSupport.parkNanos(nanos) }
  }

  override fun onThreadParkUntil(deadline: Long) =
      onThreadParkTimed(deadline) { LockSupport.parkUntil(deadline) }

  override fun onThreadParkNanosWithBlocker(blocker: Any?, nanos: Long) =
      onThreadParkTimed(
          context.timeController.currentTimeMillisRawNoIncrement() + nanos / 1_000_000) {
            LockSupport.parkNanos(blocker, nanos)
          }

  override fun onThreadParkUntilWithBlocker(blocker: Any?, deadline: Long) =
      onThreadParkTimed(deadline) { LockSupport.parkUntil(blocker, deadline) }

  inline fun <T> onConditionAwaitTimed(
      blockedUntil: Long,
      o: Condition,
      originBlock: () -> T,
      resultMapping: (Boolean) -> T,
  ): T {
    val result = runCatching {
      synchronizer.runInFrayStart("Condition.await") {
        context.conditionAwait(
            o,
            canInterrupt = true,
            blockedUntil,
        )
      }
    }
    return synchronizer.runInFrayDoneWithOriginBlock(
        "Condition.await",
        {
          result.onSuccess { runCatching { o.await() } }
          val awaitDoneResult = context.conditionAwaitDone(o, true).map(resultMapping)

          if (result.isFailure) {
            Result.failure(result.exceptionOrNull()!!)
          } else {
            awaitDoneResult
          }
        },
        originBlock,
    )
  }

  override fun onConditionAwaitTime(o: Condition, time: Long, unit: TimeUnit): Boolean =
      onConditionAwaitTimed(
          unit.toMillis(time) + context.timeController.currentTimeMillisRawNoIncrement(),
          o,
          { o.await(time, unit) },
      ) {
        it
      }

  override fun onConditionAwaitNanos(o: Condition, nanos: Long): Long =
      onConditionAwaitTimed(
          nanos / 1_000_000L + context.timeController.currentTimeMillisRawNoIncrement(),
          o,
          { o.awaitNanos(nanos) },
      ) {
        if (it) {
          (nanos - 10000000).coerceAtLeast(0)
        } else {
          0
        }
      }

  override fun onConditionAwaitUntil(o: Condition, deadline: Date): Boolean =
      onConditionAwaitTimed(
          deadline.time,
          o,
          { o.awaitUntil(deadline) },
      ) {
        it
      }

  override fun onThreadIsInterrupted(result: Boolean, t: Thread): Boolean =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          { context.threadIsInterrupted(t, result) },
          { result },
      )

  override fun onObjectHashCode(t: Any): Int =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          { context.hashCode(t) },
          { t.hashCode() },
      )

  override fun onForkJoinPoolCommonPool(pool: ForkJoinPool): ForkJoinPool =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          { context.getForkJoinPoolCommon() },
          { pool },
      )

  override fun onThreadLocalRandomGetProbe(probe: Int): Int =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          { context.getThreadLocalRandomProbe() },
          { probe },
      )

  override fun onThreadSleepDuration(duration: Duration) =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          {
            context.threadSleepOperation(
                duration.toMillis() + context.timeController.currentTimeMillisRawNoIncrement())
          },
          { Thread.sleep(duration.toMillis()) },
      )

  override fun onThreadSleepMillis(millis: Long) =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          {
            context.threadSleepOperation(
                millis + context.timeController.currentTimeMillisRawNoIncrement())
          },
          { Thread.sleep(millis) },
      )

  override fun onThreadSleepMillisNanos(millis: Long, nanos: Int) =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          {
            context.threadSleepOperation(
                millis + context.timeController.currentTimeMillisRawNoIncrement())
          },
          { Thread.sleep(millis, nanos) },
      )

  override fun onStampedLockReadLock(lock: StampedLock) =
      synchronizer.runInFrayStart("StampedLock") {
        context.stampedLockLock(
            lock,
            shouldBlock = true,
            canInterrupt = false,
            BLOCKED_OPERATION_NOT_TIMED,
            isReadLock = true,
        )
      }

  override fun onStampedLockWriteLock(lock: StampedLock) =
      synchronizer.runInFrayStart("StampedLock") {
        context.stampedLockLock(
            lock,
            shouldBlock = true,
            canInterrupt = false,
            BLOCKED_OPERATION_NOT_TIMED,
            isReadLock = false,
        )
      }

  override fun onStampedLockSkipDone() =
      synchronizer.runInFrayDone("StampedLock") { Result.success(Unit) }

  override fun onStampedLockSkip() =
      synchronizer.runInFrayStart("StampedLock") { Result.success(Unit) }

  override fun onStampedLockReadLockInterruptibly(lock: StampedLock) =
      synchronizer.runInFrayStart("StampedLock") {
        context.stampedLockLock(
            lock,
            shouldBlock = true,
            canInterrupt = true,
            BLOCKED_OPERATION_NOT_TIMED,
            isReadLock = true,
        )
      }

  override fun onStampedLockWriteLockInterruptibly(lock: StampedLock) =
      synchronizer.runInFrayStart("StampedLock") {
        context.stampedLockLock(
            lock,
            shouldBlock = true,
            canInterrupt = true,
            BLOCKED_OPERATION_NOT_TIMED,
            isReadLock = false,
        )
      }

  override fun onStampedLockUnlockReadDone(lock: StampedLock) =
      synchronizer.runInFrayDone("StampedLock") { context.stampedLockUnlock(lock, true) }

  override fun onStampedLockUnlockWriteDone(lock: StampedLock) =
      synchronizer.runInFrayDone("StampedLock") { context.stampedLockUnlock(lock, false) }

  override fun onStampedLockTryUnlockWriteDone(success: Boolean, lock: StampedLock): Boolean =
      synchronizer.runInFrayDoneWithOriginBlock(
          "StampedLock",
          {
            if (success) {
              context.stampedLockUnlock(lock, false)
            }
            Result.success(success)
          },
          { success },
      )

  override fun onStampedLockTryUnlockReadDone(success: Boolean, lock: StampedLock): Boolean =
      synchronizer.runInFrayDoneWithOriginBlock(
          "StampedLock",
          {
            if (success) {
              context.stampedLockUnlock(lock, true)
            }
            Result.success(success)
          },
          { success },
      )

  override fun onStampedLockTryConvertToWriteLockDone(
      newStamp: Long,
      lock: StampedLock,
      stamp: Long,
  ): Long =
      synchronizer.runInFrayDoneWithOriginBlock(
          "StampedLock",
          { context.stampedLockConvertToWriteLock(lock, stamp, newStamp).map { newStamp } },
          { newStamp },
      )

  override fun onStampedLockTryConvertToReadLockDone(
      newStamp: Long,
      lock: StampedLock,
      stamp: Long,
  ): Long =
      synchronizer.runInFrayDoneWithOriginBlock(
          "StampedLock",
          { context.stampedLockConvertToReadLock(lock, stamp, newStamp).map { newStamp } },
          { newStamp },
      )

  override fun onStampedLockTryConvertToOptimisticReadLockDone(
      newStamp: Long,
      lock: StampedLock,
      stamp: Long,
  ): Long =
      synchronizer.runInFrayDoneWithOriginBlock(
          "StampedLock",
          {
            context.stampedLockConvertToOptimisticReadLock(lock, stamp, newStamp).map { newStamp }
          },
          { newStamp },
      )

  override fun onStampedLockReadLockTryLock(lock: StampedLock) =
      synchronizer.runInFrayStart("StampedLock") {
        context.stampedLockLock(
            lock,
            shouldBlock = false,
            canInterrupt = false,
            BLOCKED_OPERATION_NOT_TIMED,
            isReadLock = true,
        )
      }

  override fun onStampedLockWriteLockTryLock(lock: StampedLock) =
      synchronizer.runInFrayStart("StampedLock") {
        context.stampedLockLock(
            lock,
            shouldBlock = false,
            canInterrupt = false,
            BLOCKED_OPERATION_NOT_TIMED,
            isReadLock = false,
        )
      }

  override fun onStampedLockReadLockTryLockTimeout(
      lock: StampedLock,
      timeout: Long,
      unit: TimeUnit
  ): Long =
      synchronizer.runInFrayStartWithOriginBlock(
          "StampedLock",
          {
            context
                .stampedLockLock(
                    lock,
                    shouldBlock = true,
                    canInterrupt = true,
                    blockedUntil =
                        context.timeController.currentTimeMillisRawNoIncrement() +
                            unit.toMillis(timeout),
                    isReadLock = true,
                )
                .map { 0L }
          },
          {
            return@runInFrayStartWithOriginBlock timeout
          },
      )

  override fun onStampedLockWriteLockTryLockTimeout(
      lock: StampedLock,
      timeout: Long,
      unit: TimeUnit
  ): Long =
      synchronizer.runInFrayStartWithOriginBlock(
          "StampedLock",
          {
            context
                .stampedLockLock(
                    lock,
                    shouldBlock = true,
                    canInterrupt = true,
                    blockedUntil =
                        context.timeController.currentTimeMillisRawNoIncrement() +
                            unit.toMillis(timeout),
                    isReadLock = false,
                )
                .map { 0L }
          },
          {
            return@runInFrayStartWithOriginBlock timeout
          },
      )

  override fun onRangerCondition(condition: RangerCondition) =
      synchronizer.runInFrayStartNoSkip { context.rangerCondition(condition) }

  override fun onNanoTime(): Long =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          { context.timeController.nanoTime() },
          { System.nanoTime() },
      )

  override fun onCurrentTimeMillis(): Long =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          { context.timeController.currentTimeMillis() },
          { System.currentTimeMillis() },
      )

  override fun onInstantNow(): Instant =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          { context.timeController.instantNow() },
          { Instant.now() },
      )
}
