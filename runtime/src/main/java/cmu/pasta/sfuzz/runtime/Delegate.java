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

    public void onReentrantLockTryLock(ReentrantLock l) {
    }

    public void onReentrantLockLock(ReentrantLock l) {
    }

    public void onReentrantLockUnlock(ReentrantLock l) {
    }

    public void onReentrantLockUnlockDone(ReentrantLock l) {
    }

    public void onAtomicOperation(Object o, MemoryOpType type) {
    }

    public Condition onReentrantLockNewCondition(Condition c, ReentrantLock l) {
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

