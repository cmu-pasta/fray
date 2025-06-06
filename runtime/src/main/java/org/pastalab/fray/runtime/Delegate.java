package org.pastalab.fray.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

public class Delegate {

    public void onThreadCreateDone(Thread t) {
    }

    public void onThreadStart(Thread t) {
    }

    public void onThreadStartDone(Thread t) {
    }

    public void onThreadRun() {
    }

    public void onThreadEnd() {
    }

    public void onObjectWait(Object o, long timeout) {
    }

    public void onObjectWaitDone(Object o) {
    }

    public void onObjectNotify(Object o) {
    }

    public void onObjectNotifyAll(Object o) {
    }

    public void onLockTryLock(Lock l) {
    }

    public long onLockTryLockInterruptibly(Lock l, long timeout, TimeUnit unit) {
        return unit.toMillis(timeout);
    }

    public void onLockTryLockInterruptiblyDone(Lock l) {
    }

    public void onLockTryLockDone(Lock l) {
    }

    public void onLockLock(Lock l) {
    }

    public boolean onLockHasQueuedThreads(Lock l, boolean result) {
        return result;
    }

    public boolean onLockHasQueuedThread(Lock l, Thread t, boolean result) {
        return result;
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

    public void onAtomicOperationDone() {}

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

    public boolean onSkipMethodDone(String signature) {
        return false;
    }

    public void start() {
    }

    public void onThreadPark() {
    }

    public void onUnsafeThreadParkTimed(boolean isAbsolute, long time) {
        if (isAbsolute) {
            LockSupport.parkUntil(time);
        } else {
            LockSupport.parkNanos(time);
        }
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

    public long onSemaphoreTryAcquirePermitsTimeout(Semaphore sem, int permits, long timeout, TimeUnit unit) {
        return timeout;
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

    public boolean onLatchAwaitTimeout(CountDownLatch latch, long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
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

    public void onThreadParkUntilWithBlocker(Object blocker, long until) {
        LockSupport.parkUntil(blocker, until);
    }

    public void onThreadInterruptDone(Thread t) {
    }

    public long onConditionAwaitNanos(Condition object, long nanos) throws InterruptedException {
        return object.awaitNanos(nanos);
    }

    public boolean onConditionAwaitTime(Condition object, long time, TimeUnit unit) throws InterruptedException {
        return object.await(time, unit);
    }

    public boolean onConditionAwaitUntil(Condition object, Date deadline) throws InterruptedException {
        return object.awaitUntil(deadline);
    }

    public void onConditionAwaitUninterruptibly(Condition object) {
    }

    public void onConditionAwaitUninterruptiblyDone(Condition object) {
    }

    public boolean onThreadIsInterrupted(boolean result, Thread t) {
        return result;
    }

    public int onObjectHashCode(Object t) {
        return t.hashCode();
    }

    public ForkJoinPool onForkJoinPoolCommonPool(ForkJoinPool pool) {
        return pool;
    }

    public int onThreadLocalRandomGetProbe(int probe) {
        return probe;
    }

    public void onThreadSleepMillis(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    public void onThreadSleepDuration(Duration duration) throws InterruptedException {
        Thread.sleep(duration.toMillis());
    }

    public void onThreadSleepMillisNanos(long millis, int nanos) throws InterruptedException {
        Thread.sleep(millis, nanos);
    }

    public void onStampedLockReadLock(StampedLock lock) {}

    public void onStampedLockWriteLock(StampedLock lock) {}

    public void onStampedLockReadLockInterruptibly(StampedLock lock) {}

    public void onStampedLockWriteLockInterruptibly(StampedLock lock) {}

    public void onStampedLockReadLockTryLock(StampedLock lock) {}

    public void onStampedLockWriteLockTryLock(StampedLock lock) {}

    public long onStampedLockReadLockTryLockTimeout(StampedLock lock, long timeout, TimeUnit unit) {
        return timeout;
    }

    public long onStampedLockWriteLockTryLockTimeout(StampedLock lock, long timeout, TimeUnit unit) {
        return timeout;
    }

    public void onStampedLockUnlockReadDone(StampedLock lock) {}

    public void onStampedLockUnlockWriteDone(StampedLock lock) {}

    public long onStampedLockTryConvertToReadLockDone(long newStamp, StampedLock lock, long stamp) {
        return newStamp;
    }

    public long onStampedLockTryConvertToWriteLockDone(long newStamp, StampedLock lock, long stamp) {
        return newStamp;
    }

    public long onStampedLockTryConvertToOptimisticReadLockDone(long newStamp, StampedLock lock, long stamp) {
        return newStamp;
    }

    public boolean onStampedLockTryUnlockWriteDone(boolean success, StampedLock lock) {
        return success;
    }

    public boolean onStampedLockTryUnlockReadDone(boolean success, StampedLock lock) {
        return success;
    }

    public void onStampedLockSkipDone() {
    }

    public void onStampedLockSkip() {
    }

    public void onRangerCondition(RangerCondition condition) {
    }
}

