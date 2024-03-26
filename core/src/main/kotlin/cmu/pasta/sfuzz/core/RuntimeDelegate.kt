package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.concurrency.SFuzzThread
import cmu.pasta.sfuzz.runtime.Delegate
import cmu.pasta.sfuzz.runtime.MemoryOpType
import cmu.pasta.sfuzz.runtime.TargetTerminateException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class RuntimeDelegate: Delegate() {

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
        if (Thread.currentThread() is SFuzzThread) {
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
        if (checkEntered()) return
        GlobalContext.threadStart(t)
        entered.set(false)
    }

    override fun onThreadStartDone(t: Thread) {
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
        GlobalContext.objectWait(o)
        entered.set(false)
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

    override fun onLockLock(l: ReentrantLock) {
        if (checkEntered()) {
            skipFunctionEntered.set(1 + skipFunctionEntered.get())
            return
        }
        GlobalContext.lockLock(l)
        entered.set(false)
        skipFunctionEntered.set(skipFunctionEntered.get() + 1)
    }

    override fun onLockLockDone(l: ReentrantLock?) {
        skipFunctionEntered.set(skipFunctionEntered.get() - 1)
    }

    override fun onAtomicOperation(o: Any, type: MemoryOpType) {
        if (checkEntered()) return
        GlobalContext.atomicOperation(o, type)
        entered.set(false)
    }

    override fun onLockUnlock(l: ReentrantLock) {
        if (checkEntered()) {
            skipFunctionEntered.set(1 + skipFunctionEntered.get())
            return
        }
        GlobalContext.lockUnlock(l)
        entered.set(false)
        skipFunctionEntered.set(1 + skipFunctionEntered.get())
    }

    override fun onLockUnlockDone(l: ReentrantLock) {
        skipFunctionEntered.set(skipFunctionEntered.get() - 1)
        if (checkEntered()) return
        GlobalContext.lockUnlockDone(l)
        entered.set(false)
    }

    override fun onMonitorEnter(o: Any) {
        if (checkEntered()) return
        GlobalContext.monitorEnter(o)
        entered.set(false)
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

    override fun onLockNewCondition(c: Condition, l: ReentrantLock):Condition {
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
        GlobalContext.conditionAwait(o)
        entered.set(false)
        skipFunctionEntered.set(1 + skipFunctionEntered.get())
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

    override fun onUnsafeOperation() {
        if (checkEntered()) return
//        GlobalContext.memoryOperation(null, MemoryOperation.Type.UNSAFE)
        entered.set(false)
    }

    override fun onFieldRead(o: Any, owner: String, name: String, descriptor: String) {
        if (checkEntered()) return
        GlobalContext.fieldOperation(o, owner, name, MemoryOpType.MEMORY_READ)
        entered.set(false)
    }

    override fun onFieldWrite(o: Any, owner: String, name: String, descriptor: String) {
        if (checkEntered()) return
        GlobalContext.fieldOperation(o, owner, name, MemoryOpType.MEMORY_WRITE)
        entered.set(false)
    }


    override fun onStaticFieldRead(owner: String, name: String, descriptor: String) {
        if (checkEntered()) return
        GlobalContext.fieldOperation(null, owner, name, MemoryOpType.MEMORY_READ)
        entered.set(false)
    }

    override fun onStaticFieldWrite(owner: String, name: String, descriptor: String) {
        if (checkEntered()) return
        GlobalContext.fieldOperation(null, owner, name, MemoryOpType.MEMORY_WRITE)
        entered.set(false)
    }

    override fun onExit(status: Int) {
        throw TargetTerminateException(status)
    }

    override fun onYield() {
        if (checkEntered()) return
        GlobalContext.scheduleNextOperation(true)
        entered.set(false)
    }

    override fun onLoadClass() {
        skipFunctionEntered.set(1 + skipFunctionEntered.get())
    }

    override fun onLoadClassDone() {
        skipFunctionEntered.set(skipFunctionEntered.get() - 1)
    }

    override fun onThreadPark() {
        if (checkEntered()) return
        GlobalContext.threadPark()
        entered.set(false)
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
        GlobalContext.semaphoreAcquire(sem, permits, true)
        entered.set(false)
        skipFunctionEntered.set(skipFunctionEntered.get() + 1)
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
        GlobalContext.latchAwait(latch)
        entered.set(false)
        skipFunctionEntered.set(skipFunctionEntered.get() + 1)
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

    override fun onLatchCountDownDone(latch: CountDownLatch?) {
        skipFunctionEntered.set(skipFunctionEntered.get() - 1)
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