package cmu.pasta.sfuzz.runtime;

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

    public static void onReentrantLockTryLock(Object l) {
        DELEGATE.onReentrantLockTryLock(l);
    }

    public static void onReentrantLockLock(Object l) {
        DELEGATE.onReentrantLockLock(l);
    }

    public static void onReentrantLockUnlock(Object l) {
        DELEGATE.onReentrantLockUnlock(l);
    }

    public static void onObjectWait(Object o) {
        DELEGATE.onObjectWait(o);
    }

    public static void onObjectNotify(Object o) {
        DELEGATE.onObjectNotify(o);
    }

    public static void onObjectNotifyAll(Object o) {
        DELEGATE.onObjectNotifyAll(o);
    }

    public static void onAtomicOperation(Object o) {
        DELEGATE.onAtomicOperation(o);
    }
}
