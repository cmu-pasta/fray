package cmu.pasta.sfuzz.runtime;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    public void onReentrantLockTryLock(Object l) {
    }

    public void onReentrantLockLock(Object l) {
    }

    public void onReentrantLockUnlock(Object l) {
    }

    public void onReentrantLockUnlockDone(Object l) {
    }

    public void onAtomicOperation(Object o, MemoryOpType type) {
    }

    public Condition onReentrantLockNewCondition(Condition c, ReentrantLock l) {
        return c;
    }

    public void onConditionAwait(Object l) {
    }

    public void onConditionSignal(Object l) {
    }

    public void onConditionSignalAll(Object l) {
    }

    public void onAtomicOperation(Object o) {
    }

    public void onFieldRead(Object o, String owner, String name, String descriptor) {
    }

    public void onFieldWrite(Object o, String owner, String name, String descriptor) {
    }

    public void onStaticFieldRead(String owner, String name, String descriptor) {
    }

    public void onStaticFieldWrite(String owner, String name, String descriptor) {
    }

    public void onUnsafeOperation() {
    }

    public void onExit(int status) {
        java.lang.Runtime.getRuntime().exit(0);
    }

    public void onYield() {
    }

    public void onLoadClass() {
    }

    public void onLoadClassDone() {
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
}

