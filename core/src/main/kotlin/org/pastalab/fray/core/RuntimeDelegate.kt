package org.pastalab.fray.core

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
import org.pastalab.fray.core.concurrency.HelperThread
import org.pastalab.fray.core.utils.Utils.verifyOrReport
import org.pastalab.fray.runtime.SyncurityCondition

class RuntimeDelegate(val context: RunContext) : org.pastalab.fray.runtime.Delegate() {

  var entered = ThreadLocal.withInitial { false }
  var skipFunctionEntered = ThreadLocal.withInitial { 0 }
  val stackTrace = ThreadLocal.withInitial { mutableListOf<String>() }

  private fun checkEntered(): Boolean {
    if (entered.get()) {
      return true
    }
    entered.set(true)
    if (skipFunctionEntered.get() > 0) {
      entered.set(false)
      return true
    }
    if (Thread.currentThread() is HelperThread) {
      entered.set(false)
      return true
    }
    // We do not process threads created outside of application.
    if (!context.registeredThreads.containsKey(Thread.currentThread().id)) {
      entered.set(false)
      return true
    }
    return false
  }

  override fun onMainExit() {
    context.mainCleanup()
    if (checkEntered()) return
    context.mainExit()
    entered.set(false)
  }

  override fun onThreadCreateDone(t: Thread) {
    if (checkEntered()) return
    context.threadCreateDone(t)
    entered.set(false)
  }

  override fun onThreadStart(t: Thread) {
    if (checkEntered()) {
      onSkipMethod("thread.start")
      return
    }
    context.threadStart(t)
    onSkipMethod("thread.start")
    entered.set(false)
  }

  override fun onThreadStartDone(t: Thread) {
    onSkipMethodDone("thread.start")
    if (checkEntered()) return
    context.threadStartDone(t)
    entered.set(false)
  }

  override fun onThreadRun() {
    if (checkEntered()) return
    context.threadRun()
    entered.set(false)
  }

  override fun onThreadEnd() {
    if (checkEntered()) return
    context.threadCompleted(Thread.currentThread())
    entered.set(false)
  }

  override fun onThreadGetState(t: Thread, state: Thread.State): Thread.State {
    if (checkEntered()) return state
    val result = context.threadGetState(t, state)
    entered.set(false)
    return result
  }

  override fun onObjectWait(o: Any, timeout: Long) {
    if (checkEntered()) return
    try {
      context.objectWait(o, timeout != 0L)
    } finally {
      entered.set(false)
    }
  }

  override fun onObjectWaitDone(o: Any) {
    if (checkEntered()) return
    try {
      context.objectWaitDone(o)
    } finally {
      entered.set(false)
    }
  }

  override fun onObjectNotify(o: Any) {
    if (checkEntered()) return
    context.objectNotify(o)
    entered.set(false)
  }

  override fun onObjectNotifyAll(o: Any) {
    if (checkEntered()) return
    context.objectNotifyAll(o)
    entered.set(false)
  }

  override fun onLockLockInterruptibly(l: Lock) {
    if (checkEntered()) {
      onSkipMethod("Lock.lock")
      return
    }
    try {
      context.lockLock(l, true)
    } finally {
      entered.set(false)
      onSkipMethod("Lock.lock")
    }
  }

  override fun onLockHasQueuedThreads(l: Lock, result: Boolean): Boolean {
    if (checkEntered()) {
      entered.set(false)
      return result
    }
    val result = context.lockHasQueuedThreads(l)
    entered.set(false)
    return result
  }

  override fun onLockHasQueuedThread(l: Lock, t: Thread, result: Boolean): Boolean {
    if (checkEntered()) {
      entered.set(false)
      return result
    }
    val result = context.lockHasQueuedThread(l, t)
    entered.set(false)
    return result
  }

  override fun onLockTryLock(l: Lock) {
    if (checkEntered()) {
      onSkipMethod("Lock.tryLock")
      return
    }
    try {
      context.lockTryLock(l, false, false)
    } finally {
      entered.set(false)
      onSkipMethod("Lock.tryLock")
    }
  }

  override fun onLockTryLockDone(l: Lock) {
    onSkipMethodDone("Lock.tryLock")
  }

  override fun onLockTryLockInterruptibly(l: Lock, timeout: Long): Long {
    if (checkEntered()) {
      onSkipMethod("Lock.tryLock")
      return timeout
    }
    try {
      context.lockTryLock(l, true, true)
    } finally {
      entered.set(false)
      onSkipMethod("Lock.tryLock")
    }
    return 0
  }

  override fun onLockTryLockInterruptiblyDone(l: Lock) {
    onSkipMethodDone("Lock.tryLock")
  }

  override fun onLockLock(l: Lock) {
    if (checkEntered()) {
      onSkipMethod("Lock.lock")
      return
    }
    try {
      context.lockLock(l, false)
    } finally {
      onSkipMethod("Lock.lock")
      entered.set(false)
    }
  }

  override fun onLockLockDone() {
    onSkipMethodDone("Lock.lock")
  }

  override fun onAtomicOperation(o: Any, type: org.pastalab.fray.runtime.MemoryOpType) {
    if (checkEntered()) return
    try {
      context.atomicOperation(o, type)
    } finally {
      entered.set(false)
    }
  }

  override fun onLockUnlock(l: Lock) {
    if (checkEntered()) {
      onSkipMethod("Lock.unlock")
      return
    }
    try {
      context.lockUnlock(l)
    } finally {
      entered.set(false)
      onSkipMethod("Lock.unlock")
    }
  }

  override fun onLockUnlockDone(l: Lock) {
    onSkipMethodDone("Lock.unlock")
    if (checkEntered()) return
    context.lockUnlockDone(l)
    entered.set(false)
  }

  override fun onMonitorEnter(o: Any) {
    if (checkEntered()) return
    try {
      context.monitorEnter(o)
    } finally {
      entered.set(false)
    }
  }

  override fun onMonitorExit(o: Any) {
    if (checkEntered()) return
    context.monitorExit(o)
    entered.set(false)
  }

  override fun onMonitorExitDone(o: Any) {
    if (checkEntered()) return
    context.lockUnlockDone(o)
    entered.set(false)
  }

  override fun onLockNewCondition(c: Condition, l: Lock) {
    if (checkEntered()) return
    context.lockNewCondition(c, l)
    entered.set(false)
  }

  override fun onConditionAwait(o: Condition) {
    onConditionAwaitImpl(o, true, false)
  }

  fun onConditionAwaitImpl(o: Condition, canInterrupt: Boolean, timed: Boolean): Boolean {
    if (checkEntered()) {
      onSkipMethod("Condition.await")
      return true
    }
    try {
      context.conditionAwait(o, canInterrupt, timed)
    } finally {
      entered.set(false)
      onSkipMethod("Condition.await")
    }
    return false
  }

  fun onConditionAwaitDoneImpl(o: Condition, canInterrupt: Boolean): Boolean {
    if (!onSkipMethodDone("Condition.await")) {
      return true
    }
    if (checkEntered()) return true
    try {
      return context.conditionAwaitDone(o, canInterrupt)
    } finally {
      entered.set(false)
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
    if (checkEntered()) {
      onSkipMethod("Condition.signal")
      return
    }
    context.conditionSignal(o)
    entered.set(false)
    onSkipMethod("Condition.signal")
  }

  override fun onConditionSignalDone(l: Condition) {
    onSkipMethodDone("Condition.signal")
  }

  override fun onConditionSignalAll(o: Condition) {
    if (checkEntered()) {
      onSkipMethod("Condition.signal")
      return
    }
    context.conditionSignalAll(o)
    entered.set(false)
    onSkipMethod("Condition.signal")
  }

  override fun onUnsafeReadVolatile(o: Any?, offset: Long) {
    if (o == null) return
    if (checkEntered()) return
    try {
      context.unsafeOperation(o, offset, org.pastalab.fray.runtime.MemoryOpType.MEMORY_READ)
    } finally {
      entered.set(false)
    }
  }

  override fun onUnsafeWriteVolatile(o: Any?, offset: Long) {
    if (o == null) return
    if (checkEntered()) return
    try {
      context.unsafeOperation(o, offset, org.pastalab.fray.runtime.MemoryOpType.MEMORY_WRITE)
    } finally {
      entered.set(false)
    }
  }

  override fun onFieldRead(o: Any?, owner: String, name: String, descriptor: String) {
    if (o == null) return
    if (checkEntered()) return
    try {
      context.fieldOperation(o, owner, name, org.pastalab.fray.runtime.MemoryOpType.MEMORY_READ)
    } finally {
      entered.set(false)
    }
  }

  override fun onFieldWrite(o: Any?, owner: String, name: String, descriptor: String) {
    if (o == null) return
    if (checkEntered()) return
    try {
      context.fieldOperation(o, owner, name, org.pastalab.fray.runtime.MemoryOpType.MEMORY_WRITE)
    } finally {
      entered.set(false)
    }
  }

  override fun onStaticFieldRead(owner: String, name: String, descriptor: String) {
    if (checkEntered()) return
    try {
      context.fieldOperation(null, owner, name, org.pastalab.fray.runtime.MemoryOpType.MEMORY_READ)
    } finally {
      entered.set(false)
    }
  }

  override fun onStaticFieldWrite(owner: String, name: String, descriptor: String) {
    if (checkEntered()) return
    try {
      context.fieldOperation(null, owner, name, org.pastalab.fray.runtime.MemoryOpType.MEMORY_WRITE)
    } finally {
      entered.set(false)
    }
  }

  override fun onExit(status: Int) {
    if (checkEntered()) return
    if (status != 0) {
      context.reportError(RuntimeException("Exit with status $status"))
    }
    entered.set(false)
  }

  override fun onYield() {
    if (checkEntered()) return
    try {
      context.yield()
    } finally {
      entered.set(false)
    }
  }

  val onSkipRecursion = ThreadLocal.withInitial { false }

  override fun onSkipMethod(signature: String) {
    if (onSkipRecursion.get()) {
      return
    }
    onSkipRecursion.set(true)
    if (!context.registeredThreads.containsKey(Thread.currentThread().id)) {
      onSkipRecursion.set(false)
      return
    }
    stackTrace.get().add(signature)
    skipFunctionEntered.set(1 + skipFunctionEntered.get())
    onSkipRecursion.set(false)
  }

  override fun onSkipMethodDone(signature: String): Boolean {
    if (onSkipRecursion.get()) {
      return false
    }
    onSkipRecursion.set(true)
    try {
      if (!context.registeredThreads.containsKey(Thread.currentThread().id)) {
        return false
      }
      if (stackTrace.get().isEmpty()) {
        return false
      }
      val last = stackTrace.get().removeLast()
      if (last != signature) {
        return false
      }
      skipFunctionEntered.set(skipFunctionEntered.get() - 1)
      return true
    } finally {
      onSkipRecursion.set(false)
    }
  }

  fun onThreadParkImpl(): Boolean {
    if (checkEntered()) {
      onSkipMethod("Thread.park")
      return true
    }
    try {
      context.threadPark()
    } finally {
      entered.set(false)
      onSkipMethod("Thread.park")
    }
    return false
  }

  override fun onThreadPark() {
    onThreadParkImpl()
  }

  fun onThreadParkDoneImpl(timed: Boolean) {
    if (!onSkipMethodDone("Thread.park")) {
      return
    }
    if (checkEntered()) return
    try {
      context.threadParkDone(timed)
    } finally {
      entered.set(false)
    }
  }

  override fun onThreadParkDone() {
    onThreadParkDoneImpl(false)
  }

  override fun onThreadUnpark(t: Thread?) {
    if (t == null) return
    if (checkEntered()) {
      return
    }
    context.threadUnpark(t)
    entered.set(false)
  }

  override fun onThreadUnparkDone(t: Thread?) {
    if (t == null) return
    if (checkEntered()) return
    context.threadUnparkDone(t)
    entered.set(false)
  }

  override fun onThreadInterrupt(t: Thread) {
    if (checkEntered()) return
    try {
      context.threadInterrupt(t)
    } finally {
      entered.set(false)
    }
  }

  override fun onThreadInterruptDone(t: Thread) {
    if (checkEntered()) return
    context.threadInterruptDone(t)
    entered.set(false)
  }

  override fun onThreadClearInterrupt(origin: Boolean, t: Thread): Boolean {
    if (checkEntered()) return origin
    val o = context.threadClearInterrupt(t)
    entered.set(false)
    return o
  }

  override fun onReentrantReadWriteLockInit(lock: ReentrantReadWriteLock) {
    context.reentrantReadWriteLockInit(lock)
  }

  override fun onSemaphoreInit(sem: Semaphore) {
    if (checkEntered()) return
    context.semaphoreInit(sem)
    entered.set(false)
  }

  override fun onSemaphoreTryAcquire(sem: Semaphore, permits: Int) {
    if (checkEntered()) {
      onSkipMethod("Semaphore.acquire")
      return
    }
    try {
      context.semaphoreAcquire(sem, permits, false, true, false)
    } finally {
      onSkipMethod("Semaphore.acquire")
      entered.set(false)
    }
  }

  override fun onSemaphoreTryAcquirePermitsTimeout(
      sem: Semaphore,
      permits: Int,
      timeout: Long,
      unit: TimeUnit
  ): Long {
    if (checkEntered()) {
      onSkipMethod("Semaphore.acquire")
      return timeout
    }
    try {
      context.semaphoreAcquire(sem, permits, true, true, true)
    } finally {
      onSkipMethod("Semaphore.acquire")
      entered.set(false)
    }
    return 0
  }

  override fun onSemaphoreAcquire(sem: Semaphore, permits: Int) {
    if (checkEntered()) {
      onSkipMethod("Semaphore.acquire")
      return
    }
    try {
      context.semaphoreAcquire(sem, permits, true, true, false)
    } finally {
      onSkipMethod("Semaphore.acquire")
      entered.set(false)
    }
  }

  override fun onSemaphoreAcquireUninterruptibly(sem: Semaphore, permits: Int) {
    if (checkEntered()) {
      onSkipMethod("Semaphore.acquire")
      return
    }
    try {
      context.semaphoreAcquire(sem, permits, true, false, false)
    } finally {
      entered.set(false)
      onSkipMethod("Semaphore.acquire")
    }
  }

  override fun onSemaphoreAcquireDone() {
    onSkipMethodDone("Semaphore.acquire")
  }

  override fun onSemaphoreRelease(sem: Semaphore, permits: Int) {
    if (checkEntered()) {
      onSkipMethod("Semaphore.release")
      return
    }
    context.semaphoreRelease(sem, permits)
    entered.set(false)
    onSkipMethod("Semaphore.release")
  }

  override fun onSemaphoreReleaseDone() {
    onSkipMethodDone("Semaphore.release")
  }

  override fun onSemaphoreDrainPermits(sem: Semaphore) {
    if (checkEntered()) {
      onSkipMethod("Semaphore.drainPermits")
      return
    }
    context.semaphoreDrainPermits(sem)
    entered.set(false)
    onSkipMethod("Semaphore.drainPermits")
  }

  override fun onSemaphoreDrainPermitsDone() {
    onSkipMethodDone("Semaphore.drainPermits")
  }

  override fun onSemaphoreReducePermits(sem: Semaphore, permits: Int) {
    if (checkEntered()) {
      onSkipMethod("Semaphore.reducePermits")
      return
    }
    context.semaphoreReducePermits(sem, permits)
    entered.set(false)
    onSkipMethod("Semaphore.reducePermits")
  }

  override fun onSemaphoreReducePermitsDone() {
    onSkipMethodDone("Semaphore.reducePermits")
  }

  fun onLatchAwaitImpl(latch: CountDownLatch, timed: Boolean): Boolean {
    if (checkEntered()) {
      onSkipMethod("Latch.await")
      return true
    }
    try {
      context.latchAwait(latch, timed)
    } finally {
      entered.set(false)
      onSkipMethod("Latch.await")
    }
    return false
  }

  override fun onLatchAwait(latch: CountDownLatch) {
    onLatchAwaitImpl(latch, false)
  }

  fun onLatchAwaitDoneImpl(latch: CountDownLatch): Boolean {
    onSkipMethodDone("Latch.await")
    if (checkEntered()) return true
    try {
      return context.latchAwaitDone(latch)
    } finally {
      entered.set(false)
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
    if (checkEntered()) {
      onSkipMethod("Latch.countDown")
      return
    }
    context.latchCountDown(latch)
    entered.set(false)
    onSkipMethod("Latch.countDown")
  }

  override fun onLatchCountDownDone(latch: CountDownLatch) {
    onSkipMethodDone("Latch.countDown")
    if (checkEntered()) return
    context.latchCountDownDone(latch)
    entered.set(false)
  }

  override fun onReportError(e: Throwable) {
    val originEntered = entered.get()
    entered.set(true)
    context.reportError(e)
    entered.set(originEntered)
  }

  override fun onArrayLoad(o: Any?, index: Int) {
    if (o == null) return
    if (checkEntered()) return
    try {
      context.arrayOperation(o, index, org.pastalab.fray.runtime.MemoryOpType.MEMORY_READ)
    } finally {
      entered.set(false)
    }
  }

  override fun onArrayStore(o: Any?, index: Int) {
    if (o == null) return
    if (checkEntered()) return
    try {
      context.arrayOperation(o, index, org.pastalab.fray.runtime.MemoryOpType.MEMORY_WRITE)
    } finally {
      entered.set(false)
    }
  }

  override fun start() {
    // For the first thread, it is not registered.
    // Therefor we cannot call `checkEntered` here.
    try {
      entered.set(true)
      context.start()
      entered.set(false)
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
    if (checkEntered()) return result
    val isInterrupted = context.threadIsInterrupted(t, result)
    entered.set(false)
    return isInterrupted
  }

  override fun onNanoTime(): Long {
    if (checkEntered()) return System.nanoTime()
    val value = context.nanoTime()
    entered.set(false)
    return value
  }

  override fun onCurrentTimeMillis(): Long {
    if (checkEntered()) return System.currentTimeMillis()
    val value = context.currentTimeMillis()
    entered.set(false)
    return value
  }

  override fun onInstantNow(): Instant {
    if (checkEntered()) return Instant.now()
    val instant = context.instantNow()
    entered.set(false)
    return instant
  }

  override fun onObjectHashCode(t: Any): Int {
    if (checkEntered()) return t.hashCode()
    val hashCode = context.hashCode(t)
    entered.set(false)
    return hashCode
  }

  override fun onForkJoinPoolCommonPool(pool: ForkJoinPool): ForkJoinPool {
    if (checkEntered()) return pool
    val pool = context.getForkJoinPoolCommon()
    entered.set(false)
    return pool
  }

  override fun onThreadLocalRandomGetProbe(probe: Int): Int {
    if (checkEntered()) return probe
    val probe = context.getThreadLocalRandomProbe()
    entered.set(false)
    return probe
  }

  override fun onThreadSleepDuration(duration: Duration) {
    if (checkEntered()) {
      Thread.sleep(duration.toMillis())
      return
    }
    try {
      context.threadSleepOperation()
    } finally {
      entered.set(false)
    }
  }

  override fun onThreadSleepMillis(millis: Long) {
    if (checkEntered()) {
      Thread.sleep(millis)
      return
    }
    try {
      context.threadSleepOperation()
    } finally {
      entered.set(false)
    }
  }

  override fun onThreadSleepMillisNanos(millis: Long, nanos: Int) {
    if (checkEntered()) {
      Thread.sleep(millis, nanos)
      return
    }
    try {
      context.threadSleepOperation()
    } finally {
      entered.set(false)
    }
  }

  override fun onStampedLockReadLock(lock: StampedLock) {
    if (checkEntered()) {
      onSkipMethod("StampedLock")
      return
    }
    context.stampedLockLock(lock, true, false, false, true)
    entered.set(false)
  }

  override fun onStampedLockWriteLock(lock: StampedLock) {
    if (checkEntered()) {
      onSkipMethod("StampedLock")
      return
    }
    context.stampedLockLock(lock, true, false, false, false)
    entered.set(false)
  }

  override fun onStampedLockSkipDone() {
    onSkipMethodDone("StampedLock")
  }

  override fun onStampedLockSkip() {
    onSkipMethod("StampedLock")
  }

  override fun onStampedLockReadLockInterruptibly(lock: StampedLock) {
    if (checkEntered()) {
      onSkipMethod("StampedLock")
      return
    }
    try {
      context.stampedLockLock(lock, true, true, false, true)
    } finally {
      entered.set(false)
    }
  }

  override fun onStampedLockWriteLockInterruptibly(lock: StampedLock) {
    if (checkEntered()) {
      onSkipMethod("StampedLock")
      return
    }
    try {
      context.stampedLockLock(lock, true, true, false, false)
    } finally {
      entered.set(false)
    }
  }

  override fun onStampedLockUnlockReadDone(lock: StampedLock) {
    onSkipMethodDone("StampedLock")
    if (checkEntered()) {
      return
    }
    context.stampedLockUnlock(lock, true)
    entered.set(false)
  }

  override fun onStampedLockUnlockWriteDone(lock: StampedLock) {
    onSkipMethodDone("StampedLock")
    if (checkEntered()) {
      return
    }
    context.stampedLockUnlock(lock, false)
    entered.set(false)
  }

  override fun onStampedLockTryUnlockWriteDone(success: Boolean, lock: StampedLock): Boolean {
    onSkipMethodDone("StampedLock")
    if (checkEntered()) {
      return success
    }
    if (success) {
      context.stampedLockUnlock(lock, false)
    }
    entered.set(false)
    return success
  }

  override fun onStampedLockTryUnlockReadDone(success: Boolean, lock: StampedLock): Boolean {
    onSkipMethodDone("StampedLock")
    if (checkEntered()) {
      return success
    }
    if (success) {
      context.stampedLockUnlock(lock, true)
    }
    entered.set(false)
    return success
  }

  override fun onStampedLockTryConvertToWriteLockDone(
      newStamp: Long,
      lock: StampedLock,
      stamp: Long,
  ): Long {
    onSkipMethodDone("StampedLock")
    if (checkEntered()) {
      return newStamp
    }
    context.stampedLockConvertToWriteLock(lock, stamp, newStamp)
    entered.set(false)
    return newStamp
  }

  override fun onStampedLockTryConvertToReadLockDone(
      newStamp: Long,
      lock: StampedLock,
      stamp: Long,
  ): Long {
    onSkipMethodDone("StampedLock")
    if (checkEntered()) {
      return newStamp
    }
    context.stampedLockConvertToReadLock(lock, stamp, newStamp)
    entered.set(false)
    return newStamp
  }

  override fun onStampedLockTryConvertToOptimisticReadLockDone(
      newStamp: Long,
      lock: StampedLock,
      stamp: Long,
  ): Long {
    onSkipMethodDone("StampedLock")
    if (checkEntered()) {
      return newStamp
    }
    context.stampedLockConvertToOptimisticReadLock(lock, stamp, newStamp)
    entered.set(false)
    return newStamp
  }

  override fun onStampedLockReadLockTryLock(lock: StampedLock) {
    if (checkEntered()) {
      onSkipMethod("StampedLock")
      return
    }
    context.stampedLockLock(lock, false, false, false, true)
    entered.set(false)
  }

  override fun onStampedLockWriteLockTryLock(lock: StampedLock) {
    if (checkEntered()) {
      onSkipMethod("StampedLock")
      return
    }
    context.stampedLockLock(lock, false, false, false, false)
    entered.set(false)
  }

  override fun onStampedLockReadLockTryLockTimeout(
      lock: StampedLock,
      timeout: Long,
      unit: TimeUnit
  ): Long {
    if (checkEntered()) {
      onSkipMethod("StampedLock")
      return timeout
    }
    try {
      context.stampedLockLock(lock, true, true, true, true)
    } finally {
      entered.set(false)
    }
    return 0
  }

  override fun onStampedLockWriteLockTryLockTimeout(
      lock: StampedLock,
      timeout: Long,
      unit: TimeUnit
  ): Long {
    if (checkEntered()) {
      onSkipMethod("StampedLock")
      return timeout
    }
    try {
      context.stampedLockLock(lock, true, true, true, false)
    } finally {
      entered.set(false)
    }
    return 0
  }

  override fun onSyncurityCondition(condition: SyncurityCondition) {
    if (checkEntered()) {
      verifyOrReport(false, "This method should never be called recursively")
      return
    }
    try {
      context.syncurityCondition(condition)
    } finally {
      entered.set(false)
    }
  }
}
