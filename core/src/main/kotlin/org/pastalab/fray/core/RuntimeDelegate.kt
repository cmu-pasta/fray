package org.pastalab.fray.core

import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.LockSupport
import java.util.concurrent.locks.ReentrantReadWriteLock
import org.pastalab.fray.core.concurrency.HelperThread

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
    if (checkEntered()) return
    context.mainExit()
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

  override fun onObjectWait(o: Any) {
    if (checkEntered()) return
    try {
      context.objectWait(o)
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
      context.lockTryLock(l, false)
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
      context.lockTryLock(l, true)
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
    context.monitorEnterDone(o)
    entered.set(false)
  }

  override fun onLockNewCondition(c: Condition, l: Lock) {
    if (checkEntered()) return
    context.lockNewCondition(c, l)
    entered.set(false)
  }

  override fun onConditionAwait(o: Condition) {
    if (checkEntered()) {
      onSkipMethod("Condition.await")
      return
    }
    try {
      context.conditionAwait(o, true)
    } finally {
      entered.set(false)
      onSkipMethod("Condition.await")
    }
  }

  override fun onConditionAwaitUninterruptibly(o: Condition) {
    if (checkEntered()) {
      onSkipMethod("Condition.await")
      return
    }
    try {
      context.conditionAwait(o, false)
    } finally {
      entered.set(false)
      onSkipMethod("Condition.await")
    }
  }

  override fun onConditionAwaitDone(o: Condition) {
    if (!onSkipMethodDone("Condition.await")) {
      return
    }
    if (checkEntered()) return
    try {
      context.conditionAwaitDone(o, true)
    } finally {
      entered.set(false)
    }
  }

  override fun onConditionAwaitUninterruptiblyDone(o: Condition) {
    if (!onSkipMethodDone("Condition.await")) {
      return
    }
    if (checkEntered()) return
    try {
      context.conditionAwaitDone(o, false)
    } finally {
      entered.set(false)
    }
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

  override fun onSkipMethod(signature: String) {
    if (!context.registeredThreads.containsKey(Thread.currentThread().id)) {
      return
    }
    stackTrace.get().add(signature)
    skipFunctionEntered.set(1 + skipFunctionEntered.get())
  }

  override fun onSkipMethodDone(signature: String): Boolean {
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
  }

  override fun onThreadPark() {
    if (checkEntered()) return
    try {
      context.threadPark()
    } finally {
      entered.set(false)
    }
  }

  override fun onThreadParkDone() {
    if (checkEntered()) return
    try {
      context.threadParkDone()
    } finally {
      entered.set(false)
    }
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
    if (checkEntered()) return
    context.reentrantReadWriteLockInit(lock.readLock(), lock.writeLock())
    entered.set(false)
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
      context.semaphoreAcquire(sem, permits, false, true)
    } finally {
      onSkipMethod("Semaphore.acquire")
      entered.set(false)
    }
  }

  override fun onSemaphoreAcquire(sem: Semaphore, permits: Int) {
    if (checkEntered()) {
      onSkipMethod("Semaphore.acquire")
      return
    }
    try {
      context.semaphoreAcquire(sem, permits, true, true)
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
      context.semaphoreAcquire(sem, permits, true, false)
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

  override fun onLatchAwait(latch: CountDownLatch) {
    if (checkEntered()) {
      onSkipMethod("Latch.await")
      return
    }
    try {
      context.latchAwait(latch)
    } finally {
      entered.set(false)
      onSkipMethod("Latch.await")
    }
  }

  override fun onLatchAwaitTimeout(latch: CountDownLatch, timeout: Long, unit: TimeUnit): Boolean {
    if (context.config.executionInfo.timedOpAsYield) {
      onYield()
      return false
    } else {
      latch.await()
      return true
    }
  }

  override fun onLatchAwaitDone(latch: CountDownLatch) {
    onSkipMethodDone("Latch.await")
    if (checkEntered()) return
    context.latchAwaitDone(latch)
    entered.set(false)
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
    if (checkEntered()) return
    context.reportError(e)
    entered.set(false)
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
    if (context.config.executionInfo.timedOpAsYield) {
      onYield()
    } else {
      LockSupport.park()
    }
  }

  override fun onThreadParkUntil(nanos: Long) {
    if (context.config.executionInfo.timedOpAsYield) {
      onYield()
    } else {
      LockSupport.park()
    }
  }

  override fun onThreadParkNanosWithBlocker(blocker: Any?, nanos: Long) {
    if (checkEntered()) {
      try {
        LockSupport.parkNanos(nanos)
      } finally {
        entered.set(false)
      }
    }
    entered.set(false)
    if (context.config.executionInfo.timedOpAsYield) {
      onYield()
    } else {
      LockSupport.park(blocker)
    }
  }

  override fun onThreadParkUntilWithBlocker(blocker: Any?, nanos: Long) {
    if (context.config.executionInfo.timedOpAsYield) {
      onYield()
    } else {
      LockSupport.park(blocker)
    }
  }

  override fun onConditionAwaitTime(o: Condition, time: Long, unit: TimeUnit): Boolean {
    if (context.config.executionInfo.timedOpAsYield) {
      onYield()
      return false
    } else {
      o.await()
      return true
    }
  }

  override fun onConditionAwaitNanos(o: Condition, nanos: Long): Long {
    if (checkEntered()) {
      try {
        return o.awaitNanos(nanos)
      } finally {
        entered.set(false)
      }
    }
    entered.set(false)
    if (context.config.executionInfo.timedOpAsYield) {
      onYield()
      return 0
    } else {
      o.await()
      return nanos
    }
  }

  override fun onConditionAwaitUntil(o: Condition, deadline: Date): Boolean {
    if (context.config.executionInfo.timedOpAsYield) {
      onYield()
      return false
    } else {
      o.await()
      return true
    }
  }

  override fun onThreadIsInterrupted(result: Boolean, t: Thread): Boolean {
    if (checkEntered()) return result
    val isInterrupted = context.threadIsInterrupted(t, result)
    entered.set(false)
    return isInterrupted
  }

  override fun onLockTryLockTimeout(l: Lock, timeout: Long, unit: TimeUnit): Boolean {
    if (context.config.executionInfo.timedOpAsYield) {
      onYield()
      return false
    } else {
      return l.tryLock()
    }
  }

  override fun onNanoTime(): Long {
    return context.nanoTime()
  }

  override fun onObjectHashCode(t: Any): Int {
    if (checkEntered()) return t.hashCode()
    val hashCode = context.hashCode(t)
    entered.set(false)
    return hashCode
  }
}
