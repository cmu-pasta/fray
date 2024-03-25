package cmu.pasta.sfuzz.runtime;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
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

    public static void onLockTryLock(ReentrantLock l) {
        DELEGATE.onReentrantLockTryLock(l);
    }

    public static void onLockLock(ReentrantLock l) {
        DELEGATE.onLockLock(l);
    }

    public static void onLockLockDone(ReentrantLock l) {
        DELEGATE.onLockLockDone(l);
    }

    public static void onLockUnlock(ReentrantLock l) {
        DELEGATE.onLockUnlock(l);
    }

    public static void onLockUnlockDone(ReentrantLock l) {
        DELEGATE.onLockUnlockDone(l);
    }

    public static Condition onLockNewCondition(Condition c, ReentrantLock l) {
        return DELEGATE.onLockNewCondition(c, l);
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

    public static void onConditionSignalAll(Condition o) {
        DELEGATE.onConditionSignalAll(o);
    }

    public static void onAtomicOperation(Object o, MemoryOpType type) {
        DELEGATE.onAtomicOperation(o, type);
    }

    public static void onUnsafeOperation() {
        DELEGATE.onUnsafeOperation();
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

    public static void onLoadClass() {
        DELEGATE.onLoadClass();
    }

    public static void onLoadClassDone() {
        DELEGATE.onLoadClassDone();
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
}
