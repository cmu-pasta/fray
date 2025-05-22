package org.pastalab.fray.test.core.success.cdl;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchNormalNotify {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            try {
                cdl.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t.start();
        Object o = new Object();
        synchronized (o) {
            o.wait(1000);
        }
        cdl.countDown();
        t.join();
    }
}
