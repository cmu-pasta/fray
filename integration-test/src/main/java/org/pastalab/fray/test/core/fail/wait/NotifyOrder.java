package org.pastalab.fray.test.core.fail.wait;

import org.pastalab.fray.test.ExpectedException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotifyOrder {
    public static void notifyOrder() throws InterruptedException {
        Object o = new Object();
        CountDownLatch latch = new CountDownLatch(2);
        AtomicBoolean flag = new AtomicBoolean(false);
        AtomicBoolean notifyFlag = new AtomicBoolean(false);
        AtomicBoolean bugFound = new AtomicBoolean(false);
        Thread t1 = new Thread(() -> {
            synchronized (o) {
                try {
                    latch.countDown();
                    while (!notifyFlag.get()) {
                        o.wait();
                    }
                    flag.set(true);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread t2 = new Thread(() -> {
            synchronized (o) {
                try {
                    latch.countDown();
                    while (!notifyFlag.get()) {
                        o.wait();
                    }
                    if (!flag.get()) {
                        bugFound.set(true);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t1.start();
        t2.start();
        latch.await();
        synchronized (o) {
            notifyFlag.set(true);
            o.notifyAll();
        }
        t1.join();
        t2.join();
        if (bugFound.get()) {
            throw new ExpectedException("notify order bug found");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        notifyOrder();
    }
}