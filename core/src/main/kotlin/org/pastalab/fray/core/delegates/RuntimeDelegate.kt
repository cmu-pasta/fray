package org.pastalab.fray.core.delegates

import java.time.Duration
import java.util.Date
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
import org.pastalab.fray.core.utils.Utils
import org.pastalab.fray.runtime.Delegate
import org.pastalab.fray.runtime.MemoryOpType
import org.pastalab.fray.runtime.RangerCondition

class RuntimeDelegate(val context: RunContext, val synchronizer: DelegateSynchronizer) :
    Delegate() {

  override fun onMainExit() {
    context.mainCleanup()
    if (synchronizer.checkEntered()) return
    context.mainExit()
    synchronizer.entered.set(false)
  }

  override fun onThreadCreateDone(t: Thread) {
    if (synchronizer.checkEntered()) return
    context.threadCreateDone(t)
    synchronizer.entered.set(false)
  }

  override fun onThreadStart(t: Thread) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("thread.start")
      return
    }
    context.threadStart(t)
    onSkipMethod("thread.start")
    synchronizer.entered.set(false)
  }

  override fun onThreadStartDone(t: Thread) {
    onSkipMethodDone("thread.start")
    if (synchronizer.checkEntered()) return
    context.threadStartDone(t)
    synchronizer.entered.set(false)
  }

  override fun onThreadRun() {
    if (synchronizer.checkEntered()) return
    context.threadRun()
    synchronizer.entered.set(false)
  }

  override fun onThreadEnd() {
    if (synchronizer.checkEntered()) return
    context.threadCompleted(Thread.currentThread())
    synchronizer.entered.set(false)
  }

  override fun onThreadGetState(t: Thread, state: Thread.State): Thread.State {
    if (synchronizer.checkEntered()) return state
    val result = context.threadGetState(t, state)
    synchronizer.entered.set(false)
    return result
  }

  override fun onObjectWait(o: Any, timeout: Long) {
    if (synchronizer.checkEntered()) return
    try {
      context.objectWait(o, timeout != 0L)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onObjectWaitDone(o: Any) {
    if (synchronizer.checkEntered()) return
    try {
      context.objectWaitDone(o)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onObjectNotify(o: Any) {
    if (synchronizer.checkEntered()) return
    context.objectNotify(o)
    synchronizer.entered.set(false)
  }

  override fun onObjectNotifyAll(o: Any) {
    if (synchronizer.checkEntered()) return
    context.objectNotifyAll(o)
    synchronizer.entered.set(false)
  }

  override fun onLockLockInterruptibly(l: Lock) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Lock.lock")
      return
    }
    try {
      context.lockLock(l, true)
    } finally {
      synchronizer.entered.set(false)
      onSkipMethod("Lock.lock")
    }
  }

  override fun onLockHasQueuedThreads(l: Lock, result: Boolean): Boolean {
    if (synchronizer.checkEntered()) {
      synchronizer.entered.set(false)
      return result
    }
    val result = context.lockHasQueuedThreads(l)
    synchronizer.entered.set(false)
    return result
  }

  override fun onLockHasQueuedThread(l: Lock, t: Thread, result: Boolean): Boolean {
    if (synchronizer.checkEntered()) {
      synchronizer.entered.set(false)
      return result
    }
    val result = context.lockHasQueuedThread(l, t)
    synchronizer.entered.set(false)
    return result
  }

  override fun onLockTryLock(l: Lock) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Lock.tryLock")
      return
    }
    try {
      context.lockTryLock(l, false, false)
    } finally {
      synchronizer.entered.set(false)
      onSkipMethod("Lock.tryLock")
    }
  }

  override fun onLockTryLockDone(l: Lock) {
    onSkipMethodDone("Lock.tryLock")
  }

  override fun onLockTryLockInterruptibly(l: Lock, timeout: Long): Long {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Lock.tryLock")
      return timeout
    }
    try {
      context.lockTryLock(l, true, true)
    } finally {
      synchronizer.entered.set(false)
      onSkipMethod("Lock.tryLock")
    }
    return 0
  }

  override fun onLockTryLockInterruptiblyDone(l: Lock) {
    onSkipMethodDone("Lock.tryLock")
  }

  override fun onLockLock(l: Lock) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Lock.lock")
      return
    }
    try {
      context.lockLock(l, false)
    } finally {
      onSkipMethod("Lock.lock")
      synchronizer.entered.set(false)
    }
  }

  override fun onLockLockDone() {
    onSkipMethodDone("Lock.lock")
  }

  override fun onAtomicOperation(o: Any, type: MemoryOpType) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("AtomicOperation")
      return
    }
    try {
      context.atomicOperation(o, type)
    } finally {
      synchronizer.entered.set(false)
      onSkipMethod("AtomicOperation")
    }
  }

  override fun onAtomicOperationDone() {
    onSkipMethodDone("AtomicOperation")
  }

  override fun onLockUnlock(l: Lock) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Lock.unlock")
      return
    }
    try {
      context.lockUnlock(l)
    } finally {
      synchronizer.entered.set(false)
      onSkipMethod("Lock.unlock")
    }
  }

  override fun onLockUnlockDone(l: Lock) {
    onSkipMethodDone("Lock.unlock")
    if (synchronizer.checkEntered()) return
    context.lockUnlockDone(l)
    synchronizer.entered.set(false)
  }

  override fun onMonitorEnter(o: Any) {
    if (synchronizer.checkEntered()) return
    try {
      context.monitorEnter(o, false)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onMonitorExit(o: Any) {
    if (synchronizer.checkEntered()) return
    context.monitorExit(o)
    synchronizer.entered.set(false)
  }

  override fun onMonitorExitDone(o: Any) {
    if (synchronizer.checkEntered()) return
    context.lockUnlockDone(o)
    synchronizer.entered.set(false)
  }

  override fun onLockNewCondition(c: Condition, l: Lock) {
    if (synchronizer.checkEntered()) return
    context.lockNewCondition(c, l)
    synchronizer.entered.set(false)
  }

  override fun onConditionAwait(o: Condition) {
    onConditionAwaitImpl(o, true, false)
  }

  fun onConditionAwaitImpl(o: Condition, canInterrupt: Boolean, timed: Boolean): Boolean {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Condition.await")
      return true
    }
    try {
      context.conditionAwait(o, canInterrupt, timed)
    } finally {
      synchronizer.entered.set(false)
      onSkipMethod("Condition.await")
    }
    return false
  }

  fun onConditionAwaitDoneImpl(o: Condition, canInterrupt: Boolean): Boolean {
    if (!onSkipMethodDone("Condition.await")) {
      return true
    }
    if (synchronizer.checkEntered()) return true
    try {
      return context.conditionAwaitDone(o, canInterrupt)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onConditionAwaitUninterruptibly(o: Condition) {
    onConditionAwaitImpl(o, false, false)
  }

  override fun onConditionAwaitDone(o: Condition) {
    onConditionAwaitDoneImpl(o, true)
  }

  override fun onConditionAwaitUninterruptiblyDone(o: Condition) {
    onConditionAwaitDoneImpl(o, false)
  }

  override fun onConditionSignal(o: Condition) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Condition.signal")
      return
    }
    context.conditionSignal(o)
    synchronizer.entered.set(false)
    onSkipMethod("Condition.signal")
  }

  override fun onConditionSignalDone(l: Condition) {
    onSkipMethodDone("Condition.signal")
  }

  override fun onConditionSignalAll(o: Condition) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Condition.signal")
      return
    }
    context.conditionSignalAll(o)
    synchronizer.entered.set(false)
    onSkipMethod("Condition.signal")
  }

  override fun onUnsafeReadVolatile(o: Any?, offset: Long) {
    if (o == null) return
    if (synchronizer.checkEntered()) return
    try {
      context.unsafeOperation(o, offset, MemoryOpType.MEMORY_READ)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onUnsafeWriteVolatile(o: Any?, offset: Long) {
    if (o == null) return
    if (synchronizer.checkEntered()) return
    try {
      context.unsafeOperation(o, offset, MemoryOpType.MEMORY_WRITE)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onFieldRead(o: Any?, owner: String, name: String, descriptor: String) {
    if (o == null) return
    if (synchronizer.checkEntered()) return
    try {
      context.fieldOperation(o, owner, name, MemoryOpType.MEMORY_READ)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onFieldWrite(o: Any?, owner: String, name: String, descriptor: String) {
    if (o == null) return
    if (synchronizer.checkEntered()) return
    try {
      context.fieldOperation(o, owner, name, MemoryOpType.MEMORY_WRITE)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onStaticFieldRead(owner: String, name: String, descriptor: String) {
    if (synchronizer.checkEntered()) return
    try {
      context.fieldOperation(null, owner, name, MemoryOpType.MEMORY_READ)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onStaticFieldWrite(owner: String, name: String, descriptor: String) {
    if (synchronizer.checkEntered()) return
    try {
      context.fieldOperation(null, owner, name, MemoryOpType.MEMORY_WRITE)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onExit(status: Int) {
    if (synchronizer.checkEntered()) return
    if (status != 0) {
      context.reportError(RuntimeException("Exit with status $status"))
    }
    synchronizer.entered.set(false)
  }

  override fun onYield() {
    if (synchronizer.checkEntered()) return
    try {
      context.yield()
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onSkipMethod(signature: String) {
    synchronizer.onSkipMethod(signature)
  }

  override fun onSkipMethodDone(signature: String): Boolean {
    return synchronizer.onSkipMethodDone(signature)
  }

  fun onThreadParkImpl(): Boolean {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Thread.park")
      return true
    }
    try {
      context.threadPark()
    } finally {
      synchronizer.entered.set(false)
      onSkipMethod("Thread.park")
    }
    return false
  }

  override fun onThreadPark() {
    onThreadParkImpl()
  }

  override fun onUnsafeThreadParkTimed(isAbsolute: Boolean, time: Long) {
    if (isAbsolute) {
      onThreadParkUntil(time)
    } else {
      onThreadParkNanos(time)
    }
  }

  fun onThreadParkDoneImpl(timed: Boolean) {
    if (!onSkipMethodDone("Thread.park")) {
      return
    }
    if (synchronizer.checkEntered()) return
    try {
      context.threadParkDone(timed)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onThreadParkDone() {
    onThreadParkDoneImpl(false)
  }

  override fun onThreadUnpark(t: Thread?) {
    if (t == null) return
    if (synchronizer.checkEntered()) {
      return
    }
    context.threadUnpark(t)
    synchronizer.entered.set(false)
  }

  override fun onThreadUnparkDone(t: Thread?) {
    if (t == null) return
    if (synchronizer.checkEntered()) return
    context.threadUnparkDone(t)
    synchronizer.entered.set(false)
  }

  override fun onThreadInterrupt(t: Thread) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Thread.interrupt")
      return
    }
    try {
      context.threadInterrupt(t)
    } finally {
      onSkipMethod("Thread.interrupt")
      synchronizer.entered.set(false)
    }
  }

  override fun onThreadInterruptDone(t: Thread) {
    onSkipMethodDone("Thread.interrupt")
    if (synchronizer.checkEntered()) return
    context.threadInterruptDone(t)
    synchronizer.entered.set(false)
  }

  override fun onThreadClearInterrupt(origin: Boolean, t: Thread): Boolean {
    if (synchronizer.checkEntered()) return origin
    val o = context.threadClearInterrupt(t)
    synchronizer.entered.set(false)
    return o
  }

  override fun onReentrantReadWriteLockInit(lock: ReentrantReadWriteLock) {
    context.reentrantReadWriteLockInit(lock)
  }

  override fun onSemaphoreInit(sem: Semaphore) {
    if (synchronizer.checkEntered()) return
    context.semaphoreInit(sem)
    synchronizer.entered.set(false)
  }

  override fun onSemaphoreTryAcquire(sem: Semaphore, permits: Int) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Semaphore.acquire")
      return
    }
    try {
      context.semaphoreAcquire(sem, permits, false, true, false)
    } finally {
      onSkipMethod("Semaphore.acquire")
      synchronizer.entered.set(false)
    }
  }

  override fun onSemaphoreTryAcquirePermitsTimeout(
      sem: Semaphore,
      permits: Int,
      timeout: Long,
      unit: TimeUnit
  ): Long {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Semaphore.acquire")
      return timeout
    }
    try {
      context.semaphoreAcquire(sem, permits, true, true, true)
    } finally {
      onSkipMethod("Semaphore.acquire")
      synchronizer.entered.set(false)
    }
    return 0
  }

  override fun onSemaphoreAcquire(sem: Semaphore, permits: Int) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Semaphore.acquire")
      return
    }
    try {
      context.semaphoreAcquire(sem, permits, true, true, false)
    } finally {
      onSkipMethod("Semaphore.acquire")
      synchronizer.entered.set(false)
    }
  }

  override fun onSemaphoreAcquireUninterruptibly(sem: Semaphore, permits: Int) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Semaphore.acquire")
      return
    }
    try {
      context.semaphoreAcquire(sem, permits, true, false, false)
    } finally {
      synchronizer.entered.set(false)
      onSkipMethod("Semaphore.acquire")
    }
  }

  override fun onSemaphoreAcquireDone() {
    onSkipMethodDone("Semaphore.acquire")
  }

  override fun onSemaphoreRelease(sem: Semaphore, permits: Int) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Semaphore.release")
      return
    }
    context.semaphoreRelease(sem, permits)
    synchronizer.entered.set(false)
    onSkipMethod("Semaphore.release")
  }

  override fun onSemaphoreReleaseDone() {
    onSkipMethodDone("Semaphore.release")
  }

  override fun onSemaphoreDrainPermits(sem: Semaphore) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Semaphore.drainPermits")
      return
    }
    context.semaphoreDrainPermits(sem)
    synchronizer.entered.set(false)
    onSkipMethod("Semaphore.drainPermits")
  }

  override fun onSemaphoreDrainPermitsDone() {
    onSkipMethodDone("Semaphore.drainPermits")
  }

  override fun onSemaphoreReducePermits(sem: Semaphore, permits: Int) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Semaphore.reducePermits")
      return
    }
    context.semaphoreReducePermits(sem, permits)
    synchronizer.entered.set(false)
    onSkipMethod("Semaphore.reducePermits")
  }

  override fun onSemaphoreReducePermitsDone() {
    onSkipMethodDone("Semaphore.reducePermits")
  }

  fun onLatchAwaitImpl(latch: CountDownLatch, timed: Boolean): Boolean {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Latch.await")
      return true
    }
    try {
      context.latchAwait(latch, timed)
    } finally {
      synchronizer.entered.set(false)
      onSkipMethod("Latch.await")
    }
    return false
  }

  override fun onLatchAwait(latch: CountDownLatch) {
    onLatchAwaitImpl(latch, false)
  }

  fun onLatchAwaitDoneImpl(latch: CountDownLatch): Boolean {
    onSkipMethodDone("Latch.await")
    if (synchronizer.checkEntered()) return true
    try {
      return context.latchAwaitDone(latch)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onLatchAwaitTimeout(latch: CountDownLatch, timeout: Long, unit: TimeUnit): Boolean {
    try {
      if (onLatchAwaitImpl(latch, true)) {
        onSkipMethodDone("Latch.await")
        return latch.await(timeout, unit)
      }
    } catch (e: InterruptedException) {
      // Do nothing
    }
    return onLatchAwaitDoneImpl(latch)
  }

  override fun onLatchAwaitDone(latch: CountDownLatch) {
    onLatchAwaitDoneImpl(latch)
  }

  override fun onLatchCountDown(latch: CountDownLatch) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("Latch.countDown")
      return
    }
    context.latchCountDown(latch)
    synchronizer.entered.set(false)
    onSkipMethod("Latch.countDown")
  }

  override fun onLatchCountDownDone(latch: CountDownLatch) {
    onSkipMethodDone("Latch.countDown")
    if (synchronizer.checkEntered()) return
    context.latchCountDownDone(latch)
    synchronizer.entered.set(false)
  }

  override fun onReportError(e: Throwable) {
    val originEntered = synchronizer.entered.get()
    synchronizer.entered.set(true)
    context.reportError(e)
    synchronizer.entered.set(originEntered)
  }

  override fun onArrayLoad(o: Any?, index: Int) {
    if (o == null) return
    if (synchronizer.checkEntered()) return
    try {
      context.arrayOperation(o, index, MemoryOpType.MEMORY_READ)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onArrayStore(o: Any?, index: Int) {
    if (o == null) return
    if (synchronizer.checkEntered()) return
    try {
      context.arrayOperation(o, index, MemoryOpType.MEMORY_WRITE)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun start() {
    // For the first thread, it is not registered.
    // Therefor we cannot call `synchronizer.checkEntered` here.
    try {
      synchronizer.entered.set(true)
      context.start()
      synchronizer.entered.set(false)
    } catch (e: Throwable) {
      e.printStackTrace()
    }
  }

  override fun onThreadParkNanos(nanos: Long) {
    if (onThreadParkImpl()) {
      onSkipMethodDone("Thread.park")
      LockSupport.parkNanos(nanos)
      return
    }
    try {
      LockSupport.park()
    } finally {
      onThreadParkDoneImpl(true)
    }
  }

  override fun onThreadParkUntil(deadline: Long) {
    if (onThreadParkImpl()) {
      onSkipMethodDone("Thread.park")
      LockSupport.parkUntil(deadline)
      return
    }
    try {
      LockSupport.park()
    } finally {
      onThreadParkDoneImpl(true)
    }
  }

  override fun onThreadParkNanosWithBlocker(blocker: Any?, nanos: Long) {
    if (onThreadParkImpl()) {
      onSkipMethodDone("Thread.park")
      LockSupport.parkNanos(blocker, nanos)
      return
    }
    try {
      LockSupport.park()
    } finally {
      onThreadParkDoneImpl(true)
    }
  }

  override fun onThreadParkUntilWithBlocker(blocker: Any?, deadline: Long) {
    if (onThreadParkImpl()) {
      try {
        LockSupport.parkUntil(blocker, deadline)
      } finally {
        onSkipMethodDone("Thread.park")
      }
      return
    }
    try {
      LockSupport.park()
    } finally {
      onThreadParkDoneImpl(true)
    }
  }

  override fun onConditionAwaitTime(o: Condition, time: Long, unit: TimeUnit): Boolean {
    try {
      if (onConditionAwaitImpl(o, true, true)) {
        try {
          return o.await(time, unit)
        } finally {
          onSkipMethodDone("Condition.await")
        }
      }
      o.await()
    } catch (e: Throwable) {
      onConditionAwaitDoneImpl(o, true)
      throw e
    }
    return onConditionAwaitDoneImpl(o, true)
  }

  override fun onConditionAwaitNanos(o: Condition, nanos: Long): Long {
    try {
      if (onConditionAwaitImpl(o, true, true)) {
        try {
          return o.awaitNanos(nanos)
        } finally {
          onSkipMethodDone("Condition.await")
        }
      }
      o.await()
    } catch (e: Throwable) {
      onConditionAwaitDoneImpl(o, true)
      throw e
    }
    return if (onConditionAwaitDoneImpl(o, true)) {
      (nanos - 10000000).coerceAtLeast(0)
    } else {
      0
    }
  }

  override fun onConditionAwaitUntil(o: Condition, deadline: Date): Boolean {
    try {
      if (onConditionAwaitImpl(o, true, true)) {
        try {
          return o.awaitUntil(deadline)
        } finally {
          onSkipMethodDone("Condition.await")
        }
      }
      o.await()
    } catch (e: Throwable) {
      onConditionAwaitDoneImpl(o, true)
      throw e
    }
    return onConditionAwaitDoneImpl(o, true)
  }

  override fun onThreadIsInterrupted(result: Boolean, t: Thread): Boolean {
    if (synchronizer.checkEntered()) return result
    val isInterrupted = context.threadIsInterrupted(t, result)
    synchronizer.entered.set(false)
    return isInterrupted
  }

  override fun onObjectHashCode(t: Any): Int {
    if (synchronizer.checkEntered()) return t.hashCode()
    val hashCode = context.hashCode(t)
    synchronizer.entered.set(false)
    return hashCode
  }

  override fun onForkJoinPoolCommonPool(pool: ForkJoinPool): ForkJoinPool {
    if (synchronizer.checkEntered()) return pool
    val pool = context.getForkJoinPoolCommon()
    synchronizer.entered.set(false)
    return pool
  }

  override fun onThreadLocalRandomGetProbe(probe: Int): Int {
    if (synchronizer.checkEntered()) return probe
    val probe = context.getThreadLocalRandomProbe()
    synchronizer.entered.set(false)
    return probe
  }

  override fun onThreadSleepDuration(duration: Duration) {
    if (synchronizer.checkEntered()) {
      Thread.sleep(duration.toMillis())
      return
    }
    try {
      context.threadSleepOperation()
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onThreadSleepMillis(millis: Long) {
    if (synchronizer.checkEntered()) {
      Thread.sleep(millis)
      return
    }
    try {
      context.threadSleepOperation()
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onThreadSleepMillisNanos(millis: Long, nanos: Int) {
    if (synchronizer.checkEntered()) {
      Thread.sleep(millis, nanos)
      return
    }
    try {
      context.threadSleepOperation()
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onStampedLockReadLock(lock: StampedLock) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("StampedLock")
      return
    }
    context.stampedLockLock(lock, true, false, false, true)
    synchronizer.entered.set(false)
    onSkipMethod("StampedLock")
  }

  override fun onStampedLockWriteLock(lock: StampedLock) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("StampedLock")
      return
    }
    context.stampedLockLock(lock, true, false, false, false)
    synchronizer.entered.set(false)
    onSkipMethod("StampedLock")
  }

  override fun onStampedLockSkipDone() {
    onSkipMethodDone("StampedLock")
  }

  override fun onStampedLockSkip() {
    onSkipMethod("StampedLock")
  }

  override fun onStampedLockReadLockInterruptibly(lock: StampedLock) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("StampedLock")
      return
    }
    try {
      context.stampedLockLock(lock, true, true, false, true)
    } finally {
      synchronizer.entered.set(false)
      onSkipMethod("StampedLock")
    }
  }

  override fun onStampedLockWriteLockInterruptibly(lock: StampedLock) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("StampedLock")
      return
    }
    try {
      context.stampedLockLock(lock, true, true, false, false)
    } finally {
      synchronizer.entered.set(false)
      onSkipMethod("StampedLock")
    }
  }

  override fun onStampedLockUnlockReadDone(lock: StampedLock) {
    onSkipMethodDone("StampedLock")
    if (synchronizer.checkEntered()) {
      return
    }
    context.stampedLockUnlock(lock, true)
    synchronizer.entered.set(false)
  }

  override fun onStampedLockUnlockWriteDone(lock: StampedLock) {
    onSkipMethodDone("StampedLock")
    if (synchronizer.checkEntered()) {
      return
    }
    context.stampedLockUnlock(lock, false)
    synchronizer.entered.set(false)
  }

  override fun onStampedLockTryUnlockWriteDone(success: Boolean, lock: StampedLock): Boolean {
    onSkipMethodDone("StampedLock")
    if (synchronizer.checkEntered()) {
      return success
    }
    if (success) {
      context.stampedLockUnlock(lock, false)
    }
    synchronizer.entered.set(false)
    return success
  }

  override fun onStampedLockTryUnlockReadDone(success: Boolean, lock: StampedLock): Boolean {
    onSkipMethodDone("StampedLock")
    if (synchronizer.checkEntered()) {
      return success
    }
    if (success) {
      context.stampedLockUnlock(lock, true)
    }
    synchronizer.entered.set(false)
    return success
  }

  override fun onStampedLockTryConvertToWriteLockDone(
      newStamp: Long,
      lock: StampedLock,
      stamp: Long,
  ): Long {
    onSkipMethodDone("StampedLock")
    if (synchronizer.checkEntered()) {
      return newStamp
    }
    context.stampedLockConvertToWriteLock(lock, stamp, newStamp)
    synchronizer.entered.set(false)
    return newStamp
  }

  override fun onStampedLockTryConvertToReadLockDone(
      newStamp: Long,
      lock: StampedLock,
      stamp: Long,
  ): Long {
    onSkipMethodDone("StampedLock")
    if (synchronizer.checkEntered()) {
      return newStamp
    }
    context.stampedLockConvertToReadLock(lock, stamp, newStamp)
    synchronizer.entered.set(false)
    return newStamp
  }

  override fun onStampedLockTryConvertToOptimisticReadLockDone(
      newStamp: Long,
      lock: StampedLock,
      stamp: Long,
  ): Long {
    onSkipMethodDone("StampedLock")
    if (synchronizer.checkEntered()) {
      return newStamp
    }
    context.stampedLockConvertToOptimisticReadLock(lock, stamp, newStamp)
    synchronizer.entered.set(false)
    return newStamp
  }

  override fun onStampedLockReadLockTryLock(lock: StampedLock) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("StampedLock")
      return
    }
    context.stampedLockLock(lock, false, false, false, true)
    synchronizer.entered.set(false)
    onSkipMethod("StampedLock")
  }

  override fun onStampedLockWriteLockTryLock(lock: StampedLock) {
    if (synchronizer.checkEntered()) {
      onSkipMethod("StampedLock")
      return
    }
    context.stampedLockLock(lock, false, false, false, false)
    synchronizer.entered.set(false)
    onSkipMethod("StampedLock")
  }

  override fun onStampedLockReadLockTryLockTimeout(
      lock: StampedLock,
      timeout: Long,
      unit: TimeUnit
  ): Long {
    if (synchronizer.checkEntered()) {
      onSkipMethod("StampedLock")
      return timeout
    }
    try {
      context.stampedLockLock(lock, true, true, true, true)
    } finally {
      synchronizer.entered.set(false)
      onSkipMethod("StampedLock")
    }
    return 0
  }

  override fun onStampedLockWriteLockTryLockTimeout(
      lock: StampedLock,
      timeout: Long,
      unit: TimeUnit
  ): Long {
    if (synchronizer.checkEntered()) {
      onSkipMethod("StampedLock")
      return timeout
    }
    try {
      context.stampedLockLock(lock, true, true, true, false)
    } finally {
      synchronizer.entered.set(false)
      onSkipMethod("StampedLock")
    }
    return 0
  }

  override fun onRangerCondition(condition: RangerCondition) {
    if (synchronizer.checkEntered()) {
      Utils.verifyOrReport(false, "This method should never be called recursively")
      return
    }
    try {
      context.rangerCondition(condition)
    } finally {
      synchronizer.entered.set(false)
    }
  }
}
