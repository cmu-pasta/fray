package cmu.pasta.sfuzz.runtime;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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

    public static void onReentrantLockTryLock(ReentrantLock l) {
        DELEGATE.onReentrantLockTryLock(l);
    }

    public static void onReentrantLockLock(ReentrantLock l) {
        DELEGATE.onReentrantLockLock(l);
    }

    public static void onReentrantLockUnlock(ReentrantLock l) {
        DELEGATE.onReentrantLockUnlock(l);
    }

    public static void onReentrantLockUnlockDone(ReentrantLock l) {
        DELEGATE.onReentrantLockUnlockDone(l);
    }

    public static Condition onReentrantLockNewCondition(Condition c, ReentrantLock l) {
        return DELEGATE.onReentrantLockNewCondition(c, l);
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

    public static void onThreadPark() {
    }

    public static void onThreadParkDone() {
    }

    public static void onThreadUnpark(Thread t) {
    }

    public static void onThreadUnpakDone(Thread t) {
    }
}
