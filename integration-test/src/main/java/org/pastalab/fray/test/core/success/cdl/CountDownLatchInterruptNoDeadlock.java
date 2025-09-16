package org.pastalab.fray.test.core.success.cdl;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchInterruptNoDeadlock {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Thread t = new Thread(() -> {
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                // Handle interruption
                System.out.println("Thread was interrupted while waiting.");
            }
        });
        t.start();

        Thread.yield();
        t.interrupt();
        t.join();
    }
}
