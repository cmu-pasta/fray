package org.pastalab.fray.test.core.fail.cdl;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchDeadlockUnblockSameThread {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }
}
