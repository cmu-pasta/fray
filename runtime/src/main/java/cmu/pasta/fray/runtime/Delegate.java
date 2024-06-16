package cmu.pasta.fray.runtime;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.*;

public class Delegate {

    public void onThreadStart(Thread t) {
    }

    public void onThreadStartDone(Thread t) {
    }

    public void onThreadRun() {
    }

    public void onThreadEnd() {
    }

    public void onObjectWait(Object o) {
    }

    public void onObjectWaitDone(Object o) {
    }

    public void onObjectNotify(Object o) {
    }

    public void onObjectNotifyAll(Object o) {
    }

    public void onLockTryLock(Lock l) {
    }

    public void onLockTryLockDone(Lock l) {
    }

    public void onLockLock(Lock l) {
    }

    public void onLockLockInterruptibly(Lock l) {
    }

    public void onLockLockDone() {
    }

    public void onLockUnlock(Lock l) {
    }

    public void onLockUnlockDone(Lock l) {
    }

    public void onAtomicOperation(Object o, MemoryOpType type) {
    }

    public void onLockNewCondition(Condition c, Lock l) {
    }

    public void onConditionAwait(Condition l) {
    }

    public void onConditionAwaitDone(Condition l) {
    }

    public void onConditionSignal(Condition l) {
    }

    public void onConditionSignalDone(Condition l) {
    }

    public void onConditionSignalAll(Condition l) {
    }

    public void onMonitorEnter(Object o) {
    }

    public void onMonitorExit(Object o) {
    }

    public void onMonitorExitDone(Object o) {
    }

    public void onFieldRead(Object o, String owner, String name, String descriptor) {
    }

    public void onFieldWrite(Object o, String owner, String name, String descriptor) {
    }

    public void onStaticFieldRead(String owner, String name, String descriptor) {
    }

    public void onStaticFieldWrite(String owner, String name, String descriptor) {
    }

    public void onExit(int status) {
        java.lang.Runtime.getRuntime().exit(0);
    }

    public void onYield() {
    }

    public void onSkipMethod(String signature) {
    }

    public void onSkipMethodDone(String signature) {
    }

    public void start() {
    }

    public void onThreadPark() {
    }

    public void onThreadParkDone() {
    }

    public void onThreadUnpark(Thread t) {
    }

    public void onThreadUnparkDone(Thread t) {
    }

    public void onMainExit() {
    }

    public void onThreadInterrupt(Thread t) {
    }

    public Thread.State onThreadGetState(Thread t, Thread.State state) {
        return state;
    }

    public void onReentrantReadWriteLockInit(ReentrantReadWriteLock lock) {
    }

    public void onSemaphoreInit(Semaphore sem) {
    }

    public void onSemaphoreAcquire(Semaphore sem, int permits) {
    }

    public void onSemaphoreTryAcquire(Semaphore sem, int permits) {
    }

    public void onSemaphoreAcquireUninterruptibly(Semaphore sem, int permits) {
    }

    public void onSemaphoreAcquireDone() {
    }

    public void onSemaphoreDrainPermits(Semaphore sem) {
    }

    public void onSemaphoreDrainPermitsDone() {
    }

    public void onSemaphoreRelease(Semaphore sem, int permits) {
    }

    public void onSemaphoreReleaseDone() {
    }

    public void onSemaphoreReducePermits(Semaphore sem, int permits) {
    }

    public void onSemaphoreReducePermitsDone() {
    }

    public void onLatchAwait(CountDownLatch latch) {
    }

    public void onLatchAwaitDone(CountDownLatch latch) {
    }

    public void onLatchCountDown(CountDownLatch latch) {
    }

    public void onLatchCountDownDone(CountDownLatch latch) {
    }

    public boolean onThreadClearInterrupt(Boolean originValue, Thread t) {
        return originValue;
    }

    public void onReportError(Throwable e) {
    }

    public void onUnsafeReadVolatile(Object o, long offset) {
    }

    public void onUnsafeWriteVolatile(Object o, long offset) {
    }

    public void onArrayLoad(Object o, int index) {
    }

    public void onArrayStore(Object o, int index) {
    }

    public void onThreadParkNanos(long nanos) {
        LockSupport.parkNanos(nanos);
    }

    public void onThreadParkNanosWithBlocker(Object blocker, long nanos) {
        LockSupport.parkNanos(blocker, nanos);
    }

    public void onThreadParkUntil(long nanos) {
        LockSupport.parkUntil(nanos);
    }

    public void onThreadParkUntilWithBlocker(Object blocker, long nanos) {
        LockSupport.parkUntil(blocker, nanos);
    }

    public void onThreadInterruptDone(Thread t) {
    }
}

