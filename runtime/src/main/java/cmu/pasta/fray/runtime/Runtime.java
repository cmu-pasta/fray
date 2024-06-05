package cmu.pasta.fray.runtime;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// No recursion is allowed in Runtime
public class Runtime {
    public static Delegate DELEGATE = new Delegate();

    public static void onThreadStart(Thread t) {
        DELEGATE.onThreadStart(t);
    }

    public static void onThreadStartDone(Thread t) {
        DELEGATE.onThreadStartDone(t);
    }

    // onThreadEnd and onThreadRun will only be called from JVM
    // so no recursion check is necessary.
    public static void onThreadEnd() {
        DELEGATE.onThreadEnd();
    }

    public static void onThreadRun() {
        DELEGATE.onThreadRun();
    }

    public static void onLockTryLock(Lock l) {
        DELEGATE.onLockTryLock(l);
    }

    public static void onLockTryLockDone(Lock l) {
        DELEGATE.onLockTryLockDone(l);
    }

    public static void onLockLock(Lock l) {
        DELEGATE.onLockLock(l);
    }

    public static void onLockLockDone(Lock l) {
        DELEGATE.onLockLockDone(l);
    }

    public static void onLockUnlock(Lock l) {
        DELEGATE.onLockUnlock(l);
    }

    public static void onLockUnlockDone(Lock l) {
        DELEGATE.onLockUnlockDone(l);
    }

    public static void onLockNewCondition(Condition c, Lock l) {
        DELEGATE.onLockNewCondition(c, l);
    }

    public static void onObjectWait(Object o) {
        DELEGATE.onObjectWait(o);
    }

    public static void onObjectWaitDone(Object o) {
        DELEGATE.onObjectWaitDone(o);
    }

    public static void onObjectNotify(Object o) {
        DELEGATE.onObjectNotify(o);
    }

    public static void onObjectNotifyAll(Object o) {
        DELEGATE.onObjectNotifyAll(o);
    }

    public static void onConditionAwait(Condition o) {
        DELEGATE.onConditionAwait(o);
    }

    public static void onConditionAwaitDone(Condition o) {
        DELEGATE.onConditionAwaitDone(o);
    }

    public static void onConditionSignal(Condition o) {
        DELEGATE.onConditionSignal(o);
    }

    public static void onConditionSignalDone(Condition o) {
        DELEGATE.onConditionSignalDone(o);
    }

    public static void onConditionSignalAll(Condition o) {
        DELEGATE.onConditionSignalAll(o);
    }

    public static void onAtomicOperation(Object o, MemoryOpType type) {
        DELEGATE.onAtomicOperation(o, type);
    }

    public static void onArrayLoad(Object o, int index) {
        DELEGATE.onArrayLoad(o, index);
    }

    public static void onArrayStore(Object o, int index) {
        DELEGATE.onArrayStore(o, index);
    }

    public static void onFieldRead(Object o, String owner, String name, String descriptor) {
        DELEGATE.onFieldRead(o, owner, name, descriptor);
    }

    public static void onFieldWrite(Object o, String owner, String name, String descriptor) {
        DELEGATE.onFieldWrite(o, owner, name, descriptor);
    }

    public static void onStaticFieldRead(String owner, String name, String descriptor) {
        DELEGATE.onStaticFieldRead(owner, name, descriptor);
    }

    public static void onStaticFieldWrite(String owner, String name, String descriptor) {
        DELEGATE.onStaticFieldWrite(owner, name, descriptor);
    }

    public static void onMonitorEnter(Object o) {
        DELEGATE.onMonitorEnter(o);
    }

    public static void onMonitorExit(Object o) {
        DELEGATE.onMonitorExit(o);
    }

    public static void onMonitorExitDone(Object o) {
        DELEGATE.onMonitorExitDone(o);
    }

    public static void onExit(int code) {
        DELEGATE.onExit(code);
    }

    public static void onYield() {
        DELEGATE.onYield();
    }

    public static void onSkipMethod() {
        DELEGATE.onSkipMethod();
    }

    public static void onSkipMethodDone() {
        DELEGATE.onSkipMethodDone();
    }

    public static void start() {
        DELEGATE.start();
    }

    public static void onMainExit() {
        DELEGATE.onMainExit();
    }

    public static void onThreadPark() {
        DELEGATE.onThreadPark();
    }

    public static void onThreadParkDone() {
        DELEGATE.onThreadParkDone();
    }

    public static void onThreadUnpark(Thread t) {
        DELEGATE.onThreadUnpark(t);
    }

    public static void onThreadUnparkDone(Thread t) {
        DELEGATE.onThreadUnparkDone(t);
    }

    public static void onThreadInterrupt(Thread t) {
        DELEGATE.onThreadInterrupt(t);
    }

    public static boolean onThreadGetAndClearInterrupt(boolean originValue, Thread t) {
        return DELEGATE.onThreadClearInterrupt(originValue, t);
    }

    public static void onThreadClearInterrupt(Thread t) {
        DELEGATE.onThreadClearInterrupt(false, t);
    }

    public static void onReentrantReadWriteLockInit(ReentrantReadWriteLock lock) {
        DELEGATE.onReentrantReadWriteLockInit(lock);
    }

    public static void onSemaphoreInit(Semaphore sem) {
        DELEGATE.onSemaphoreInit(sem);
    }

    public static void onSemaphoreAcquirePermits(Semaphore sem, int permits) {
        DELEGATE.onSemaphoreAcquire(sem, permits);
    }

    public static void onSemaphoreAcquire(Semaphore sem) {
        DELEGATE.onSemaphoreAcquire(sem, 1);
    }

    public static void onSemaphoreTryAcquire(Semaphore sem) {
        DELEGATE.onSemaphoreTryAcquire(sem, 1);
    }

    public static void onSemaphoreTryAcquirePermits(Semaphore sem, int permits) {
        DELEGATE.onSemaphoreTryAcquire(sem, permits);
    }

    public static void onSemaphoreAcquireUninterruptibly(Semaphore sem) {
        DELEGATE.onSemaphoreAcquireUninterruptibly(sem, 1);
    }

    public static void onSemaphoreAcquireDone() {
        DELEGATE.onSemaphoreAcquireDone();
    }

    public static void onSemaphoreReleasePermits(Semaphore sem, int permits) {
        DELEGATE.onSemaphoreRelease(sem, permits);
    }

    public static void onSemaphoreRelease(Semaphore sem) {
        DELEGATE.onSemaphoreRelease(sem, 1);
    }

    public static void onSemaphoreReleaseDone() {
        DELEGATE.onSemaphoreReleaseDone();
    }

    public static void onSemaphoreDrainPermitsDone() {
        DELEGATE.onSemaphoreDrainPermitsDone();
    }

    public static void onSemaphoreReducePermits(Semaphore sem, int permits) {
        DELEGATE.onSemaphoreReducePermits(sem, permits);
    }

    public static void onSemaphoreReducePermitsDone() {
        DELEGATE.onSemaphoreReducePermitsDone();
    }

    public static void onSemaphoreDrainPermits(Semaphore sem) {
        DELEGATE.onSemaphoreDrainPermits(sem);
    }

    public static void onLatchAwait(CountDownLatch latch) {
        DELEGATE.onLatchAwait(latch);
    }

    public static void onLatchAwaitDone(CountDownLatch latch) {
        DELEGATE.onLatchAwaitDone(latch);
    }

    public static void onLatchCountDown(CountDownLatch latch) {
        DELEGATE.onLatchCountDown(latch);
    }

    public static void onLatchCountDownDone(CountDownLatch latch) {
        DELEGATE.onLatchCountDownDone(latch);
    }

    public static void onReportError(Throwable e) {
        DELEGATE.onReportError(e);
    }

    public static void onSemaphoreAcquirePermitsUninterruptibly(Semaphore sem, int permits) {
        DELEGATE.onSemaphoreAcquireUninterruptibly(sem, permits);
    }

    public static void onLockLockInterruptibly(Lock l) {
        DELEGATE.onLockLockInterruptibly(l);
    }

    public static void onUnsafeReadVolatile(Object o, long offset) {
        DELEGATE.onUnsafeReadVolatile(o, offset);
    }

    public static void onUnsafeWriteVolatile(Object o, long offset) {
        DELEGATE.onUnsafeWriteVolatile(o, offset);
    }

}
