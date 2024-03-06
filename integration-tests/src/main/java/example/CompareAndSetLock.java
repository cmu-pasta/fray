package example;

import java.util.concurrent.atomic.AtomicInteger;

public class CompareAndSetLock {
    private final AtomicInteger state = new AtomicInteger(0);
    private Thread lockOwner;
    
    public void lock() {
        while(!state.compareAndSet(0, 1));
        lockOwner = Thread.currentThread();
    }

    public void unlock() {
        if (lockOwner == Thread.currentThread()) {
            lockOwner = null;
            state.set(0);
        } else {
            throw new IllegalMonitorStateException();
        }
    }
}
