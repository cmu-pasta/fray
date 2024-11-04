package org.pastalab.fray.test.success.wait;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchAwaitTimeoutNoDeadlock {
    public static void main(String[] args) {
        CountDownLatch cdl = new CountDownLatch(1);
        try {
            cdl.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
