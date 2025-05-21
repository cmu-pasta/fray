package org.pastalab.fray.test.success.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockTryLockNoDeadlock {
    public static void main(String[] args) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch latch = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            lock.lock();
            latch.countDown();
        });

        t.start();
        latch.await();
        lock.tryLock(1000, TimeUnit.MICROSECONDS);
    }
}
