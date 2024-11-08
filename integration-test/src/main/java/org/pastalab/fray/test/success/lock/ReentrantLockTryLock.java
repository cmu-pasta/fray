package org.pastalab.fray.test.success.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockTryLock {
    public static void main(String[] args) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        Thread t = new Thread(() -> {
            lock.lock();
            Thread.yield();
            lock.unlock();
        });

        t.start();
        Boolean result = lock.tryLock(1000, TimeUnit.MICROSECONDS);
        assert(result == true);
        Thread.yield();
        lock.unlock();
    }
}
