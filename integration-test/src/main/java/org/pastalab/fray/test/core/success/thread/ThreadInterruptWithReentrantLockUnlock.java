package org.pastalab.fray.test.core.success.thread;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadInterruptWithReentrantLockUnlock {

    public static void main(String[] args) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        CountDownLatch countDownLatch = new CountDownLatch(1);



        Thread t1 = new Thread(() -> {
            lock.lock();
            try {
                countDownLatch.countDown();
                condition.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                if (lock.tryLock(1000, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
            }
        });

        t1.start();
        t2.start();

        countDownLatch.await();
        lock.lock();
        condition.signal();
        lock.unlock();

        t2.interrupt();

    }
}
