package cmu.pasta.sfuzz.runtime;


import java.util.concurrent.locks.ReentrantLock;

public class Runtime {
    public static Delegate DELEGATE = new Delegate();
    public static void onThreadStart(Thread t) {
        DELEGATE.onThreadStart(t);
    }

    public static void onThreadStartDone(Thread t) {
        DELEGATE.onThreadStartDone(t);
    }

    public static void onThreadEnd() {
        DELEGATE.onThreadEnd();
    }

    public static void onThreadRun() {
        DELEGATE.onThreadRun();
    }

    public static void onReentrantLockTryLock(ReentrantLock l) {
    }

    public static void onReentrantLockLock(ReentrantLock l) {
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

    public static void onObjectNotifyDone(Object o) {
        DELEGATE.onObjectNotifyDone(o);
    }
}
