package org.pastalab.fray.test.core.fail.cdl;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchDeadlockUnblockMultiThread {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        t.start();
        latch.await();
        t.join();
    }
}
