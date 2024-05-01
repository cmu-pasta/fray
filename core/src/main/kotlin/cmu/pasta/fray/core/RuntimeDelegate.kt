package cmu.pasta.fray.core

import cmu.pasta.fray.core.concurrency.HelperThread
import cmu.pasta.fray.runtime.Delegate
import cmu.pasta.fray.runtime.MemoryOpType
import cmu.pasta.fray.runtime.TargetTerminateException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

class RuntimeDelegate : Delegate() {

  var entered = ThreadLocal.withInitial { false }
  var skipFunctionEntered = ThreadLocal.withInitial { 0 }

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
    if (!GlobalContext.registeredThreads.containsKey(Thread.currentThread().id)) {
      entered.set(false)
      return true
    }
    return false
  }

  override fun onMainExit() {
    if (checkEntered()) return
    GlobalContext.mainExit()
    entered.set(false)
  }

  override fun onThreadStart(t: Thread) {
    if (checkEntered()) {
      skipFunctionEntered.set(1 + skipFunctionEntered.get())
      return
    }
    GlobalContext.threadStart(t)
    skipFunctionEntered.set(1 + skipFunctionEntered.get())
    entered.set(false)
  }

  override fun onThreadStartDone(t: Thread) {
    skipFunctionEntered.set(skipFunctionEntered.get() - 1)
    if (checkEntered()) return
    GlobalContext.threadStartDone(t)
    entered.set(false)
  }

  override fun onThreadRun() {
    if (checkEntered()) return
    GlobalContext.threadRun()
    entered.set(false)
  }

  override fun onThreadEnd() {
    if (checkEntered()) return
    GlobalContext.threadCompleted(Thread.currentThread())
    entered.set(false)
  }

  override fun onObjectWait(o: Any) {
    if (checkEntered()) return
    try {
      GlobalContext.objectWait(o)
    } finally {
      entered.set(false)
    }
  }

  override fun onObjectWaitDone(o: Any) {
    if (checkEntered()) return
    try {
      GlobalContext.objectWaitDone(o)
    } finally {
      entered.set(false)
    }
  }

  override fun onObjectNotify(o: Any) {
    if (checkEntered()) return
    GlobalContext.objectNotify(o)
    entered.set(false)
  }

  override fun onObjectNotifyAll(o: Any) {
    if (checkEntered()) return
    GlobalContext.objectNotifyAll(o)
    entered.set(false)
  }

  override fun onLockLockInterruptibly(l: Lock) {
    if (checkEntered()) {
      skipFunctionEntered.set(1 + skipFunctionEntered.get())
      return
    }
    try {
      GlobalContext.lockLock(l, true)
    } finally {
      entered.set(false)
      skipFunctionEntered.set(skipFunctionEntered.get() + 1)
    }
  }

  override fun onLockTryLock(l: Lock) {
    if (checkEntered()) {
      skipFunctionEntered.set(1 + skipFunctionEntered.get())
      return
    }
    try {
      GlobalContext.lockTryLock(l)
    } finally {
      entered.set(false)
      skipFunctionEntered.set(skipFunctionEntered.get() + 1)
    }
  }

  override fun onLockTryLockDone(l: Lock) {
    skipFunctionEntered.set(skipFunctionEntered.get() - 1)
  }

  override fun onLockLock(l: Lock) {
    if (checkEntered()) {
      skipFunctionEntered.set(1 + skipFunctionEntered.get())
      return
    }
    try {
      GlobalContext.lockLock(l, false)
    } finally {
      skipFunctionEntered.set(skipFunctionEntered.get() + 1)
      entered.set(false)
    }
  }

  override fun onLockLockDone(l: Lock?) {
    skipFunctionEntered.set(skipFunctionEntered.get() - 1)
  }

  override fun onAtomicOperation(o: Any, type: MemoryOpType) {
    if (checkEntered()) return
    try {
      GlobalContext.atomicOperation(o, type)
    } finally {
      entered.set(false)
    }
  }

  override fun onLockUnlock(l: Lock) {
    if (checkEntered()) {
      skipFunctionEntered.set(1 + skipFunctionEntered.get())
      return
    }
    GlobalContext.lockUnlock(l)
    entered.set(false)
    skipFunctionEntered.set(1 + skipFunctionEntered.get())
  }

  override fun onLockUnlockDone(l: Lock) {
    skipFunctionEntered.set(skipFunctionEntered.get() - 1)
    if (checkEntered()) return
    GlobalContext.lockUnlockDone(l)
    entered.set(false)
  }

  override fun onMonitorEnter(o: Any) {
    if (checkEntered()) return
    try {
      GlobalContext.monitorEnter(o)
    } finally {
      entered.set(false)
    }
  }

  override fun onMonitorExit(o: Any) {
    if (checkEntered()) return
    GlobalContext.monitorExit(o)
    entered.set(false)
  }

  override fun onMonitorExitDone(o: Any) {
    if (checkEntered()) return
    GlobalContext.monitorEnterDone(o)
    entered.set(false)
  }

  override fun onLockNewCondition(c: Condition, l: Lock): Condition {
    if (checkEntered()) return c
    GlobalContext.lockNewCondition(c, l)
    entered.set(false)
    return c
  }

  override fun onConditionAwait(o: Condition) {
    if (checkEntered()) {
      skipFunctionEntered.set(1 + skipFunctionEntered.get())
      return
    }
    try {
      GlobalContext.conditionAwait(o)
    } finally {
      entered.set(false)
      skipFunctionEntered.set(1 + skipFunctionEntered.get())
    }
  }

  override fun onConditionAwaitDone(o: Condition) {
    skipFunctionEntered.set(skipFunctionEntered.get() - 1)
    if (checkEntered()) return
    try {
      GlobalContext.conditionAwaitDone(o)
    } finally {
      entered.set(false)
    }
  }

  override fun onConditionSignal(o: Condition) {
    if (checkEntered()) return
    GlobalContext.conditionSignal(o)
    entered.set(false)
  }

  override fun onConditionSignalAll(o: Condition) {
    if (checkEntered()) return
    GlobalContext.conditionSignalAll(o)
    entered.set(false)
  }

  override fun onUnsafeReadVolatile(o: Any?, offset: Long) {
    if (o == null) return
    if (checkEntered()) return
    try {
      GlobalContext.unsafeOperation(o, offset, MemoryOpType.MEMORY_READ)
    } finally {
      entered.set(false)
    }
  }

  override fun onUnsafeWriteVolatile(o: Any?, offset: Long) {
    if (o == null) return
    if (checkEntered()) return
    try {
      GlobalContext.unsafeOperation(o, offset, MemoryOpType.MEMORY_WRITE)
    } finally {
      entered.set(false)
    }
  }

  override fun onFieldRead(o: Any, owner: String, name: String, descriptor: String) {
    if (checkEntered()) return
    try {
      GlobalContext.fieldOperation(o, owner, name, MemoryOpType.MEMORY_READ)
    } finally {
      entered.set(false)
    }
  }

  override fun onFieldWrite(o: Any, owner: String, name: String, descriptor: String) {
    if (checkEntered()) return
    try {
      GlobalContext.fieldOperation(o, owner, name, MemoryOpType.MEMORY_WRITE)
    } finally {
      entered.set(false)
    }
  }

  override fun onStaticFieldRead(owner: String, name: String, descriptor: String) {
    if (checkEntered()) return
    try {
      GlobalContext.fieldOperation(null, owner, name, MemoryOpType.MEMORY_READ)
    } finally {
      entered.set(false)
    }
  }

  override fun onStaticFieldWrite(owner: String, name: String, descriptor: String) {
    if (checkEntered()) return
    try {
      GlobalContext.fieldOperation(null, owner, name, MemoryOpType.MEMORY_WRITE)
    } finally {
      entered.set(false)
    }
  }

  override fun onExit(status: Int) {
    throw TargetTerminateException(status)
  }

  override fun onYield() {
    if (checkEntered()) return
    try {
      GlobalContext.yield()
    } finally {
      entered.set(false)
    }
  }

  override fun onSkipMethod() {
    skipFunctionEntered.set(1 + skipFunctionEntered.get())
  }

  override fun onSkipMethodDone() {
    skipFunctionEntered.set(skipFunctionEntered.get() - 1)
  }

  override fun onThreadPark() {
    if (checkEntered()) return
    try {
      GlobalContext.threadPark()
    } finally {
      entered.set(false)
    }
  }

  override fun onThreadParkDone() {
    if (checkEntered()) return
    GlobalContext.threadParkDone()
    entered.set(false)
  }

  override fun onThreadUnpark(t: Thread?) {
    if (t == null) return
    if (checkEntered()) return
    GlobalContext.threadUnpark(t)
    entered.set(false)
  }

  override fun onThreadUnparkDone(t: Thread?) {
    if (t == null) return
    if (checkEntered()) return
    GlobalContext.threadUnparkDone(t)
    entered.set(false)
  }

  override fun onThreadInterrupt(t: Thread) {
    if (checkEntered()) return
    GlobalContext.threadInterrupt(t)
    entered.set(false)
  }

  override fun onThreadClearInterrupt(origin: Boolean, t: Thread): Boolean {
    if (checkEntered()) return origin
    val o = GlobalContext.threadClearInterrupt(t)
    entered.set(false)
    return o
  }

  override fun onReentrantReadWriteLockInit(lock: ReentrantReadWriteLock) {
    if (checkEntered()) return
    GlobalContext.reentrantReadWriteLockInit(lock.readLock(), lock.writeLock())
    entered.set(false)
  }

  override fun onSemaphoreInit(sem: Semaphore) {
    if (checkEntered()) return
    GlobalContext.semaphoreInit(sem)
    entered.set(false)
  }

  override fun onSemaphoreAcquire(sem: Semaphore, permits: Int) {
    if (checkEntered()) {
      skipFunctionEntered.set(1 + skipFunctionEntered.get())
      return
    }
    try {
      GlobalContext.semaphoreAcquire(sem, permits, true, true)
    } finally {
      skipFunctionEntered.set(skipFunctionEntered.get() + 1)
      entered.set(false)
    }
  }

  override fun onSemaphoreAcquireUninterruptibly(sem: Semaphore, permits: Int) {
    if (checkEntered()) {
      skipFunctionEntered.set(1 + skipFunctionEntered.get())
      return
    }
    try {
      GlobalContext.semaphoreAcquire(sem, permits, true, false)
    } finally {
      entered.set(false)
      skipFunctionEntered.set(skipFunctionEntered.get() + 1)
    }
  }

  override fun onSemaphoreAcquireDone() {
    skipFunctionEntered.set(skipFunctionEntered.get() - 1)
  }

  override fun onSemaphoreRelease(sem: Semaphore, permits: Int) {
    if (checkEntered()) {
      skipFunctionEntered.set(1 + skipFunctionEntered.get())
      return
    }
    GlobalContext.semaphoreRelease(sem, permits)
    entered.set(false)
    skipFunctionEntered.set(skipFunctionEntered.get() + 1)
  }

  override fun onSemaphoreReleaseDone() {
    skipFunctionEntered.set(skipFunctionEntered.get() - 1)
  }

  override fun onSemaphoreDrainPermits(sem: Semaphore) {
    if (checkEntered()) {
      skipFunctionEntered.set(1 + skipFunctionEntered.get())
      return
    }
    GlobalContext.semaphoreDrainPermits(sem)
    entered.set(false)
    skipFunctionEntered.set(skipFunctionEntered.get() + 1)
  }

  override fun onSemaphoreDrainPermitsDone() {
    skipFunctionEntered.set(skipFunctionEntered.get() - 1)
  }

  override fun onSemaphoreReducePermits(sem: Semaphore, permits: Int) {
    if (checkEntered()) {
      skipFunctionEntered.set(1 + skipFunctionEntered.get())
      return
    }
    GlobalContext.semaphoreReducePermits(sem, permits)
    entered.set(false)
    skipFunctionEntered.set(skipFunctionEntered.get() + 1)
  }

  override fun onSemaphoreReducePermitsDone() {
    skipFunctionEntered.set(skipFunctionEntered.get() - 1)
  }

  override fun onLatchAwait(latch: CountDownLatch) {
    if (checkEntered()) {
      skipFunctionEntered.set(1 + skipFunctionEntered.get())
      return
    }
    try {
      GlobalContext.latchAwait(latch)
    } finally {
      entered.set(false)
      skipFunctionEntered.set(skipFunctionEntered.get() + 1)
    }
  }

  override fun onLatchAwaitDone(latch: CountDownLatch) {
    skipFunctionEntered.set(skipFunctionEntered.get() - 1)
    if (checkEntered()) return
    GlobalContext.latchAwaitDone(latch)
    entered.set(false)
  }

  override fun onLatchCountDown(latch: CountDownLatch) {
    if (checkEntered()) {
      skipFunctionEntered.set(1 + skipFunctionEntered.get())
      return
    }
    GlobalContext.latchCountDown(latch)
    entered.set(false)
    skipFunctionEntered.set(skipFunctionEntered.get() + 1)
  }

  override fun onLatchCountDownDone(latch: CountDownLatch) {
    skipFunctionEntered.set(skipFunctionEntered.get() - 1)
    if (checkEntered()) return
    GlobalContext.latchCountDownDone(latch)
    entered.set(false)
  }

  override fun onReportError(e: Throwable) {
    if (checkEntered()) return
    GlobalContext.reportError(e)
    entered.set(false)
  }

  override fun onArrayLoad(o: Any?, index: Int) {
    if (o == null) return
    if (checkEntered()) return
    try {
      GlobalContext.arrayOperation(o, index, MemoryOpType.MEMORY_READ)
    } finally {
      entered.set(false)
    }
  }

  override fun onArrayStore(o: Any?, index: Int) {
    if (o == null) return
    if (checkEntered()) return
    try {
      GlobalContext.arrayOperation(o, index, MemoryOpType.MEMORY_WRITE)
    } finally {
      entered.set(false)
    }
  }

  override fun start() {
    // For the first thread, it is not registered.
    // Therefor we cannot call `checkEntered` here.
    try {
      entered.set(true)
      GlobalContext.start()
      entered.set(false)
    } catch (e: Throwable) {
      e.printStackTrace()
    }
  }
}