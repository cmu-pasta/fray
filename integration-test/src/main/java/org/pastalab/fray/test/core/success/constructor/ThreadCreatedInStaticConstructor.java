package org.pastalab.fray.test.core.success.constructor;

import java.util.concurrent.CountDownLatch;

public class ThreadCreatedInStaticConstructor {
    public static Object o = new Object();
    public static CountDownLatch latch = new CountDownLatch(1);


    public static Thread thread = new Thread(() -> {
        synchronized (o) {
            try {
                latch.countDown();
                o.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    });

    static {
        thread.start();
    }

    public static void main(String[] args) throws InterruptedException {
        latch.await();
        synchronized (o) {
            o.notify();
        }
    }
}
