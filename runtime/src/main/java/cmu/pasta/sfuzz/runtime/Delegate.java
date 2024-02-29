package cmu.pasta.sfuzz.runtime;

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
        synchronized (o) {
            try {
                o.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void onObjectNotify(Object o) {
        synchronized (o) {
            o.notify();
        }
    }

    public void onObjectNotifyAll(Object o) {
        synchronized (o) {
            o.notifyAll();
        }
    }

    public void onReentrantLockTryLock(Object l) {
    }

    public void onReentrantLockLock(Object l) {
    }

    public void onReentrantLockUnlock(Object l) {
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

}

