package org.anonlab.fray.test.fail.thread;

import java.util.concurrent.CountDownLatch;

public class ThreadExitDeadlock {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Thread t = new Thread(() -> {
        });
        synchronized (t) {
            t.start();
            latch.await();
        }
    }
}
