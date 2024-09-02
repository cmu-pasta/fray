package org.pastalab.fray.runtime;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
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

    public static long onLockTryLockInterruptibly(Lock l, long timeout, TimeUnit unit) {
        return DELEGATE.onLockTryLockInterruptibly(l, timeout);
    }

    public static void onLockTryLockInterruptiblyDone(Lock l) {
        DELEGATE.onLockTryLockInterruptiblyDone(l);
    }

    public static void onLockTryLockDone(Lock l) {
        DELEGATE.onLockTryLockDone(l);
    }

    public static boolean onLockTryLockTimeout(Lock l, long timeout, TimeUnit unit) throws InterruptedException {
        return DELEGATE.onLockTryLockTimeout(l, timeout, unit);
    }

    public static void onLockLock(Lock l) {
        DELEGATE.onLockLock(l);
    }

    public static void onLockLockDone() {
        DELEGATE.onLockLockDone();
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

    public static void onSkipMethod(String signature) {
        DELEGATE.onSkipMethod(signature);
    }

    public static void onSkipMethodDone(String signature) {
        DELEGATE.onSkipMethodDone(signature);
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

    public static void onThreadInterruptDone(Thread t) {
        DELEGATE.onThreadInterruptDone(t);
    }

    public static Thread.State onThreadGetState(Thread.State state, Thread t) {
        return DELEGATE.onThreadGetState(t, state);
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

    public static boolean onLatchAwaitTimeout(CountDownLatch latch, long timeout, TimeUnit unit) throws InterruptedException {
        return DELEGATE.onLatchAwaitTimeout(latch, timeout, unit);
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

    public static void onThreadParkNanos(long nanos) {
        DELEGATE.onThreadParkNanos(nanos);
    }

    public static void onThreadParkUntil(long deadline) {
        DELEGATE.onThreadParkUntil(deadline);
    }

    public static void onThreadParkNanosWithBlocker(Object blocker, long nanos) {
        DELEGATE.onThreadParkNanosWithBlocker(blocker, nanos);
    }

    public static void onThreadParkUntilWithBlocker(Object blocker, long deadline) {
        DELEGATE.onThreadParkUntilWithBlocker(blocker, deadline);
    }

    public static long onConditionAwaitNanos(Condition object, long nanos) throws InterruptedException {
        return DELEGATE.onConditionAwaitNanos(object, nanos);
    }

    public static boolean onConditionAwaitTime(Condition object, long time, TimeUnit unit) throws InterruptedException {
        return DELEGATE.onConditionAwaitTime(object, time, unit);
    }

    public static boolean onConditionAwaitUntil(Condition object, Date deadline) throws InterruptedException {
        return DELEGATE.onConditionAwaitUntil(object, deadline);
    }

    public static void onConditionAwaitUninterruptibly(Condition object) {
        DELEGATE.onConditionAwaitUninterruptibly(object);
    }

    public static void onConditionAwaitUninterruptiblyDone(Condition object) {
        DELEGATE.onConditionAwaitUninterruptiblyDone(object);
    }

    public static boolean onThreadIsInterrupted(boolean result, Thread t) {
        return DELEGATE.onThreadIsInterrupted(result, t);
    }

    public static boolean onLockHasQueuedThreads(boolean result, Lock l) {
        return DELEGATE.onLockHasQueuedThreads(l, result);
    }

    public static boolean onLockHasQueuedThread(boolean result, Lock l, Thread t) {
        return DELEGATE.onLockHasQueuedThread(l, t, result);
    }

    public static long onNanoTime() {
        return DELEGATE.onNanoTime();
    }

    public static int onObjectHashCode(Object t) {
        return DELEGATE.onObjectHashCode(t);
    }

    public static ForkJoinPool onForkJoinPoolCommonPool(ForkJoinPool pool) {
        return DELEGATE.onForkJoinPoolCommonPool(pool);
    }

    public static int onThreadLocalRandomGetProbe(int probe) {
        return DELEGATE.onThreadLocalRandomGetProbe(probe);
    }

    public static void onThreadSleepMillis(long millis) throws InterruptedException {
        DELEGATE.onThreadSleepMillis(millis);
    }

    public static void onThreadSleepDuration(Duration duration) throws InterruptedException {
        DELEGATE.onThreadSleepDuration(duration);
    }

    public static void onThreadSleepMillisNanos(long millis, int nanos) throws InterruptedException {
        DELEGATE.onThreadSleepMillisNanos(millis, nanos);
    }
}
