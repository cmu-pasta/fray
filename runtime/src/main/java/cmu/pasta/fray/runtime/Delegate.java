package cmu.pasta.fray.runtime;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    public void onReentrantLockTryLock(ReentrantLock l) {
    }

    public void onLockLock(ReentrantLock l) {
    }
    public void onLockLockInterruptibly(ReentrantLock l) {
    }

    public void onLockLockDone(ReentrantLock l) {
    }

    public void onLockUnlock(ReentrantLock l) {
    }

    public void onLockUnlockDone(ReentrantLock l) {
    }

    public void onAtomicOperation(Object o, MemoryOpType type) {
    }

    public Condition onLockNewCondition(Condition c, ReentrantLock l) {
        return c;
    }

    public void onConditionAwait(Condition l) {
    }

    public void onConditionAwaitDone(Condition l) {
    }

    public void onConditionSignal(Condition l) {
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

    public void onSkipMethod() {
    }

    public void onSkipMethodDone() {
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

    public void onReentrantReadWriteLockInit(ReentrantReadWriteLock lock) {
    }

    public void onSemaphoreInit(Semaphore sem) {
    }

    public void onSemaphoreAcquire(Semaphore sem, int permits) {
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
}

