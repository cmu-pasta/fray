package org.pastalab.fray.test.success.cdl;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchAwaitTimeoutNoDeadlockSimple {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(1);
        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(() -> {
                try {
                    cdl.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
                    cdl.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            t.start();
        }
    }
}
