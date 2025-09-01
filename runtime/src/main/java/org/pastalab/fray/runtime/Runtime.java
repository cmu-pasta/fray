package org.pastalab.fray.runtime;

import java.io.PipedInputStream;
import java.net.SocketAddress;
import java.net.SocketImpl;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

// No recursion is allowed in Runtime
public class Runtime {
    public static Delegate LOCK_DELEGATE = new Delegate();
    public static NetworkDelegate NETWORK_DELEGATE = new NetworkDelegate();
    public static TimeDelegate TIME_DELEGATE = new TimeDelegate();

    public static void resetAllDelegate() {
        LOCK_DELEGATE = new Delegate();
        NETWORK_DELEGATE = new NetworkDelegate();
        TIME_DELEGATE = new TimeDelegate();
    }

    public static void onThreadCreateDone(Thread t) {
        LOCK_DELEGATE.onThreadCreateDone(t);
    }

    public static void onThreadStart(Thread t) {
        LOCK_DELEGATE.onThreadStart(t);
    }

    public static void onThreadStartDone(Thread t) {
        LOCK_DELEGATE.onThreadStartDone(t);
    }

    // onThreadEnd and onThreadRun will only be called from JVM.
    public static void onThreadEnd() {
        LOCK_DELEGATE.onThreadEnd();
    }

    public static void onThreadRun() {
        LOCK_DELEGATE.onThreadRun();
    }

    public static void onLockTryLock(Lock l) {
        LOCK_DELEGATE.onLockTryLock(l);
    }

    public static long onLockTryLockInterruptibly(Lock l, long timeout, TimeUnit unit) {
        return LOCK_DELEGATE.onLockTryLockInterruptibly(l, timeout, unit);
    }

    public static void onLockTryLockInterruptiblyDone(Lock l) {
        LOCK_DELEGATE.onLockTryLockInterruptiblyDone(l);
    }

    public static void onLockTryLockDone(Lock l) {
        LOCK_DELEGATE.onLockTryLockDone(l);
    }

    public static void onLockLock(Lock l) {
        LOCK_DELEGATE.onLockLock(l);
    }

    public static void onLockLockDone() {
        LOCK_DELEGATE.onLockLockDone();
    }

    public static void onLockUnlock(Lock l) {
        LOCK_DELEGATE.onLockUnlock(l);
    }

    public static void onLockUnlockDone(Lock l) {
        LOCK_DELEGATE.onLockUnlockDone(l);
    }

    public static void onLockNewCondition(Condition c, Lock l) {
        LOCK_DELEGATE.onLockNewCondition(c, l);
    }

    public static void onObjectWait(Object o, long timeout) {
        LOCK_DELEGATE.onObjectWait(o, timeout);
    }

    public static void onObjectWaitDone(Object o) {
        LOCK_DELEGATE.onObjectWaitDone(o);
    }

    public static void onObjectNotify(Object o) {
        LOCK_DELEGATE.onObjectNotify(o);
    }

    public static void onObjectNotifyAll(Object o) {
        LOCK_DELEGATE.onObjectNotifyAll(o);
    }

    public static void onConditionAwait(Condition o) {
        LOCK_DELEGATE.onConditionAwait(o);
    }

    public static void onConditionAwaitDone(Condition o) {
        LOCK_DELEGATE.onConditionAwaitDone(o);
    }

    public static void onConditionSignal(Condition o) {
        LOCK_DELEGATE.onConditionSignal(o);
    }

    public static void onConditionSignalDone(Condition o) {
        LOCK_DELEGATE.onConditionSignalDone(o);
    }

    public static void onConditionSignalAll(Condition o) {
        LOCK_DELEGATE.onConditionSignalAll(o);
    }

    public static void onAtomicOperation(Object o, MemoryOpType type) {
        LOCK_DELEGATE.onAtomicOperation(o, type);
    }

    public static void onAtomicOperationDone() {
        LOCK_DELEGATE.onAtomicOperationDone();
    }

    public static void onArrayLoad(Object o, int index) {
        LOCK_DELEGATE.onArrayLoad(o, index);
    }

    public static void onArrayStore(Object o, int index) {
        LOCK_DELEGATE.onArrayStore(o, index);
    }

    public static void onFieldRead(Object o, String owner, String name, String descriptor) {
        LOCK_DELEGATE.onFieldRead(o, owner, name, descriptor);
    }

    public static void onFieldWrite(Object o, String owner, String name, String descriptor) {
        LOCK_DELEGATE.onFieldWrite(o, owner, name, descriptor);
    }

    public static void onStaticFieldRead(String owner, String name, String descriptor) {
        LOCK_DELEGATE.onStaticFieldRead(owner, name, descriptor);
    }

    public static void onStaticFieldWrite(String owner, String name, String descriptor) {
        LOCK_DELEGATE.onStaticFieldWrite(owner, name, descriptor);
    }

    public static void onMonitorEnter(Object o) {
        LOCK_DELEGATE.onMonitorEnter(o);
    }

    public static void onMonitorExit(Object o) {
        LOCK_DELEGATE.onMonitorExit(o);
    }

    public static void onMonitorExitDone(Object o) {
        LOCK_DELEGATE.onMonitorExitDone(o);
    }

    public static void onExit(int code) {
        LOCK_DELEGATE.onExit(code);
    }

    public static void onYield() {
        LOCK_DELEGATE.onYield();
    }

    public static void onSkipScheduling(String signature) {
        LOCK_DELEGATE.onSkipScheduling(signature);
    }

    public static void onSkipPrimitive(String signature) {
        LOCK_DELEGATE.onSkipPrimitive(signature);
    }

    public static void onSkipPrimitiveDone(String signature) {
        LOCK_DELEGATE.onSkipPrimitiveDone(signature);
    }

    public static void onSkipSchedulingDone(String signature) {
        LOCK_DELEGATE.onSkipSchedulingDone(signature);
    }

    public static void start() {
        LOCK_DELEGATE.start();
    }

    public static void onMainExit() {
        LOCK_DELEGATE.onMainExit();
    }

    public static void onThreadPark() {
        LOCK_DELEGATE.onThreadPark();
    }

    public static void onUnsafeThreadParkTimed(boolean isAbsolute, long time) {
        LOCK_DELEGATE.onUnsafeThreadParkTimed(isAbsolute, time);
    }

    public static void onThreadParkDone() {
        LOCK_DELEGATE.onThreadParkDone();
    }

    public static void onThreadUnpark(Thread t) {
        LOCK_DELEGATE.onThreadUnpark(t);
    }

    public static void onThreadUnparkDone(Thread t) {
        LOCK_DELEGATE.onThreadUnparkDone(t);
    }

    public static void onThreadInterrupt(Thread t) {
        LOCK_DELEGATE.onThreadInterrupt(t);
    }

    public static void onThreadInterruptDone(Thread t) {
        LOCK_DELEGATE.onThreadInterruptDone(t);
    }

    public static Thread.State onThreadGetState(Thread.State state, Thread t) {
        return LOCK_DELEGATE.onThreadGetState(t, state);
    }

    public static boolean onThreadGetAndClearInterrupt(boolean originValue, Thread t) {
        return LOCK_DELEGATE.onThreadClearInterrupt(originValue, t);
    }

    public static void onThreadClearInterrupt(Thread t) {
        LOCK_DELEGATE.onThreadClearInterrupt(false, t);
    }

    public static void onReentrantReadWriteLockInit(ReentrantReadWriteLock lock) {
        LOCK_DELEGATE.onReentrantReadWriteLockInit(lock);
    }

    public static void onSemaphoreInit(Semaphore sem) {
        LOCK_DELEGATE.onSemaphoreInit(sem);
    }

    public static void onSemaphoreAcquirePermits(Semaphore sem, int permits) {
        LOCK_DELEGATE.onSemaphoreAcquire(sem, permits);
    }

    public static void onSemaphoreAcquire(Semaphore sem) {
        LOCK_DELEGATE.onSemaphoreAcquire(sem, 1);
    }

    public static void onSemaphoreTryAcquire(Semaphore sem) {
        LOCK_DELEGATE.onSemaphoreTryAcquire(sem, 1);
    }

    public static void onSemaphoreTryAcquirePermits(Semaphore sem, int permits) {
        LOCK_DELEGATE.onSemaphoreTryAcquire(sem, permits);
    }

    public static long onSemaphoreTryAcquirePermitsTimeout(Semaphore sem, int permits, long timeout, TimeUnit unit) {
        return LOCK_DELEGATE.onSemaphoreTryAcquirePermitsTimeout(sem, permits, timeout, unit);
    }

    public static long onSemaphoreTryAcquireTimeout(Semaphore sem, long timeout, TimeUnit unit) {
        return LOCK_DELEGATE.onSemaphoreTryAcquirePermitsTimeout(sem, 1, timeout, unit);
    }

    public static void onSemaphoreAcquireUninterruptibly(Semaphore sem) {
        LOCK_DELEGATE.onSemaphoreAcquireUninterruptibly(sem, 1);
    }

    public static void onSemaphoreAcquireDone() {
        LOCK_DELEGATE.onSemaphoreAcquireDone();
    }

    public static void onSemaphoreReleasePermits(Semaphore sem, int permits) {
        LOCK_DELEGATE.onSemaphoreRelease(sem, permits);
    }

    public static void onSemaphoreRelease(Semaphore sem) {
        LOCK_DELEGATE.onSemaphoreRelease(sem, 1);
    }

    public static void onSemaphoreReleaseDone() {
        LOCK_DELEGATE.onSemaphoreReleaseDone();
    }

    public static void onSemaphoreDrainPermitsDone() {
        LOCK_DELEGATE.onSemaphoreDrainPermitsDone();
    }

    public static void onSemaphoreReducePermits(Semaphore sem, int permits) {
        LOCK_DELEGATE.onSemaphoreReducePermits(sem, permits);
    }

    public static void onSemaphoreReducePermitsDone() {
        LOCK_DELEGATE.onSemaphoreReducePermitsDone();
    }

    public static void onSemaphoreDrainPermits(Semaphore sem) {
        LOCK_DELEGATE.onSemaphoreDrainPermits(sem);
    }

    public static void onLatchAwait(CountDownLatch latch) {
        LOCK_DELEGATE.onLatchAwait(latch);
    }

    public static boolean onLatchAwaitTimeout(CountDownLatch latch, long timeout, TimeUnit unit) throws InterruptedException {
        return LOCK_DELEGATE.onLatchAwaitTimeout(latch, timeout, unit);
    }

    public static void onLatchAwaitDone(CountDownLatch latch) {
        LOCK_DELEGATE.onLatchAwaitDone(latch);
    }

    public static void onLatchCountDown(CountDownLatch latch) {
        LOCK_DELEGATE.onLatchCountDown(latch);
    }

    public static void onLatchCountDownDone(CountDownLatch latch) {
        LOCK_DELEGATE.onLatchCountDownDone(latch);
    }

    public static void onReportError(Throwable e) {
        LOCK_DELEGATE.onReportError(e);
    }

    public static void onSemaphoreAcquirePermitsUninterruptibly(Semaphore sem, int permits) {
        LOCK_DELEGATE.onSemaphoreAcquireUninterruptibly(sem, permits);
    }

    public static void onLockLockInterruptibly(Lock l) {
        LOCK_DELEGATE.onLockLockInterruptibly(l);
    }

    public static void onUnsafeReadVolatile(Object o, long offset) {
        LOCK_DELEGATE.onUnsafeReadVolatile(o, offset);
    }

    public static void onUnsafeWriteVolatile(Object o, long offset) {
        LOCK_DELEGATE.onUnsafeWriteVolatile(o, offset);
    }

    public static void onThreadParkNanos(long nanos) {
        LOCK_DELEGATE.onThreadParkNanos(nanos);
    }

    public static void onThreadParkUntil(long deadline) {
        LOCK_DELEGATE.onThreadParkUntil(deadline);
    }

    public static void onThreadParkNanosWithBlocker(Object blocker, long nanos) {
        LOCK_DELEGATE.onThreadParkNanosWithBlocker(blocker, nanos);
    }

    public static void onThreadParkUntilWithBlocker(Object blocker, long deadline) {
        LOCK_DELEGATE.onThreadParkUntilWithBlocker(blocker, deadline);
    }

    public static long onConditionAwaitNanos(Condition object, long nanos) throws InterruptedException {
        return LOCK_DELEGATE.onConditionAwaitNanos(object, nanos);
    }

    public static boolean onConditionAwaitTime(Condition object, long time, TimeUnit unit) throws InterruptedException {
        return LOCK_DELEGATE.onConditionAwaitTime(object, time, unit);
    }

    public static boolean onConditionAwaitUntil(Condition object, Date deadline) throws InterruptedException {
        return LOCK_DELEGATE.onConditionAwaitUntil(object, deadline);
    }

    public static void onConditionAwaitUninterruptibly(Condition object) {
        LOCK_DELEGATE.onConditionAwaitUninterruptibly(object);
    }

    public static void onConditionAwaitUninterruptiblyDone(Condition object) {
        LOCK_DELEGATE.onConditionAwaitUninterruptiblyDone(object);
    }

    public static boolean onThreadIsInterrupted(boolean result, Thread t) {
        return LOCK_DELEGATE.onThreadIsInterrupted(result, t);
    }

    public static boolean onLockHasQueuedThreads(boolean result, Lock l) {
        return LOCK_DELEGATE.onLockHasQueuedThreads(l, result);
    }

    public static boolean onLockHasQueuedThread(boolean result, Lock l, Thread t) {
        return LOCK_DELEGATE.onLockHasQueuedThread(l, t, result);
    }

    public static long onNanoTime() {
        return TIME_DELEGATE.onNanoTime();
    }

    public static long onCurrentTimeMillis() {
        return TIME_DELEGATE.onCurrentTimeMillis();
    }

    public static Instant onInstantNow() {
        return TIME_DELEGATE.onInstantNow();
    }

    public static int onObjectHashCode(Object t) {
        return LOCK_DELEGATE.onObjectHashCode(t);
    }

    public static ForkJoinPool onForkJoinPoolCommonPool(ForkJoinPool pool) {
        return LOCK_DELEGATE.onForkJoinPoolCommonPool(pool);
    }

    public static int onThreadLocalRandomGetProbe(int probe) {
        return LOCK_DELEGATE.onThreadLocalRandomGetProbe(probe);
    }

    public static void onThreadSleepMillis(long millis) throws InterruptedException {
        LOCK_DELEGATE.onThreadSleepMillis(millis);
    }

    public static void onThreadSleepDuration(Duration duration) throws InterruptedException {
        LOCK_DELEGATE.onThreadSleepDuration(duration);
    }

    public static void onThreadSleepMillisNanos(long millis, int nanos) throws InterruptedException {
        LOCK_DELEGATE.onThreadSleepMillisNanos(millis, nanos);
    }

    public static void onStampedLockReadLock(StampedLock lock) {
        LOCK_DELEGATE.onStampedLockReadLock(lock);
    }

    public static void onStampedLockSkipDone() {
        LOCK_DELEGATE.onStampedLockSkipDone();
    }

    public static void onStampedLockWriteLock(StampedLock lock) {
        LOCK_DELEGATE.onStampedLockWriteLock(lock);
    }

    public static void onStampedLockReadLockInterruptibly(StampedLock lock) {
        LOCK_DELEGATE.onStampedLockReadLockInterruptibly(lock);
    }

    public static void onStampedLockWriteLockInterruptibly(StampedLock lock) {
        LOCK_DELEGATE.onStampedLockWriteLockInterruptibly(lock);
    }

    public static void onStampedLockReadLockTryLock(StampedLock lock) {
        LOCK_DELEGATE.onStampedLockReadLockTryLock(lock);
    }

    public static void onStampedLockWriteLockTryLock(StampedLock lock) {
        LOCK_DELEGATE.onStampedLockWriteLockTryLock(lock);
    }

    public static long onStampedLockReadLockTryLockTimeout(StampedLock lock, long timeout, TimeUnit unit) {
        return LOCK_DELEGATE.onStampedLockReadLockTryLockTimeout(lock, timeout, unit);
    }

    public static long onStampedLockWriteLockTryLockTimeout(StampedLock lock, long timeout, TimeUnit unit) {
        return LOCK_DELEGATE.onStampedLockWriteLockTryLockTimeout(lock, timeout, unit);
    }

    public static void onStampedLockUnlockReadDone(StampedLock lock) {
        LOCK_DELEGATE.onStampedLockUnlockReadDone(lock);
    }

    public static void onStampedLockUnlockWriteDone(StampedLock lock) {
        LOCK_DELEGATE.onStampedLockUnlockWriteDone(lock);
    }

    public static long onStampedLockTryConvertToReadLockDone(long newStamp, StampedLock lock, long stamp) {
        return LOCK_DELEGATE.onStampedLockTryConvertToReadLockDone(newStamp, lock, stamp);
    }

    public static long onStampedLockTryConvertToWriteLockDone(long newStamp, StampedLock lock, long stamp) {
        return LOCK_DELEGATE.onStampedLockTryConvertToWriteLockDone(newStamp, lock, stamp);
    }

    public static long onStampedLockTryConvertToOptimisticReadLockDone(long newStamp, StampedLock lock, long stamp) {
        return LOCK_DELEGATE.onStampedLockTryConvertToOptimisticReadLockDone(newStamp, lock, stamp);
    }

    public static boolean onStampedLockTryUnlockWriteDone(boolean success, StampedLock lock) {
        return LOCK_DELEGATE.onStampedLockTryUnlockWriteDone(success, lock);
    }

    public static boolean onStampedLockTryUnlockReadDone(boolean success, StampedLock lock) {
        return LOCK_DELEGATE.onStampedLockTryUnlockReadDone(success, lock);
    }

    public static void onStampedLockSkip() {
        LOCK_DELEGATE.onStampedLockSkip();
    }

    public static void onRangerCondition(RangerCondition condition) {
        LOCK_DELEGATE.onRangerCondition(condition);
    }

    public static void onSelectorOpen() {
        NETWORK_DELEGATE.onSelectorOpen();
    }

    public static void onSelectorOpenDone() {
        NETWORK_DELEGATE.onSelectorOpenDone();
    }

    public static void onSelectorSelect(Selector selector) {
        NETWORK_DELEGATE.onSelectorSelect(selector);
    }

    public static void onSelectorClose(Selector selector) {
        NETWORK_DELEGATE.onSelectorClose(selector);
    }

    public static void onSelectorCloseDone(Selector selector) {
        NETWORK_DELEGATE.onSelectorCloseDone(selector);
    }

    public static void onServerSocketChannelBindDone(ServerSocketChannel channel) {
        NETWORK_DELEGATE.onServerSocketChannelBindDone(channel);
    }

    public static void onServerSocketChannelAccept(ServerSocketChannel channel) {
        NETWORK_DELEGATE.onServerSocketChannelAccept(channel);
    }

    public static void onSocketChannelConnect(SocketChannel channel, SocketAddress remoteAddress) {
        NETWORK_DELEGATE.onSocketChannelConnect(channel, remoteAddress);
    }

    public static void onSelectorSetEventOpsDone(Selector selector, SelectionKey key) {
        NETWORK_DELEGATE.onSelectorSetEventOpsDone(selector, key);
    }

    public static void onSelectorCancelKeyDone(Selector selector, SelectionKey key) {
        NETWORK_DELEGATE.onSelectorCancelKeyDone(selector, key);
    }

    public static void onSelectorSelectDone(Selector selector) {
        NETWORK_DELEGATE.onSelectorSelectDone(selector);
    }

    public static void onServerSocketChannelAcceptDone(ServerSocketChannel channel, SocketChannel client) {
        NETWORK_DELEGATE.onServerSocketChannelAcceptDone(channel, client);
    }

    public static void onSocketChannelClose(AbstractInterruptibleChannel channel) {
        NETWORK_DELEGATE.onSocketChannelClose(channel);
    }

    public static void onSocketChannelCloseDone(AbstractInterruptibleChannel channel) {
        NETWORK_DELEGATE.onSocketChannelCloseDone(channel);
    }

    public static void onSocketChannelConnectDone(SocketChannel channel, boolean success) {
        NETWORK_DELEGATE.onSocketChannelConnectDone(channel, success);
    }

    public static void onSocketChannelFinishConnectDone(SocketChannel channel, boolean success) {
        NETWORK_DELEGATE.onSocketChannelFinishConnectDone(channel, success);
    }

    public static void onSocketChannelFinishConnect(SocketChannel channel) {
        NETWORK_DELEGATE.onSocketChannelFinishConnect(channel);
    }


    public static void onSocketChannelRead(SocketChannel channel) {
        NETWORK_DELEGATE.onSocketChannelRead(channel);
    }

    public static void onSocketChannelReadDoneInt(int bytesRead, SocketChannel channel) {
        NETWORK_DELEGATE.onSocketChannelReadDone(channel, bytesRead);
    }

    public static void onSocketChannelReadDone(long bytesRead, SocketChannel channel) {
        NETWORK_DELEGATE.onSocketChannelReadDone(channel, bytesRead);
    }

    public static void onSocketChannelWriteDoneInt(int bytesWritten, SocketChannel channel) {
        NETWORK_DELEGATE.onSocketChannelWriteDone(channel, bytesWritten);
    }

    public static void onSocketChannelWriteDone(long bytesWritten, SocketChannel channel) {
        NETWORK_DELEGATE.onSocketChannelWriteDone(channel, bytesWritten);
    }

    public static void onNioSocketConnect(SocketImpl socket) {
        NETWORK_DELEGATE.onNioSocketConnect(socket);
    }

    public static void onNioSocketConnectDone(SocketImpl socket) {
        NETWORK_DELEGATE.onNioSocketConnectDone(socket);
    }

    public static void onNioSocketRead(SocketImpl socket) {
        NETWORK_DELEGATE.onNioSocketRead(socket);
    }

    public static void onNioSocketReadDone(SocketImpl socket, int bytesRead) {
        NETWORK_DELEGATE.onNioSocketReadDone(socket, bytesRead);
    }

    public static void onNioSocketAccept(SocketImpl socket) {
        NETWORK_DELEGATE.onNioSocketAccept(socket);
    }

    public static void onNioSocketAcceptDone(SocketImpl socket) {
        NETWORK_DELEGATE.onNioSocketAcceptDone(socket);
    }

    public static void onPipedInputStreamRead(PipedInputStream inputStream) {
        NETWORK_DELEGATE.onPipedInputStreamRead(inputStream);
    }

    public static void onPipedInputStreamReadDone(PipedInputStream inputStream) {
        NETWORK_DELEGATE.onPipedInputStreamReadDone(inputStream);
    }
}
