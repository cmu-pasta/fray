package org.pastalab.fray.test.core.success.wait;

import java.util.concurrent.CountDownLatch;

public class WaitWithNotify {
    public static void main(String[] args) {
        Object o = new Object();
        CountDownLatch latch = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            synchronized (o) {
                try {
                    latch.countDown();
                    o.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        try {
            latch.await(); // Wait for the thread to start
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        synchronized (o) {
            o.notify();
        }
    }
}
