package org.pastalab.fray.test.fail.park;

import org.pastalab.fray.test.ExpectedException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class ParkSpuriousWakeup {
    public static void main(String[] args) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean flag = new AtomicBoolean(false);
        Thread t = new Thread(() -> {
            latch.countDown();
            LockSupport.park();
            flag.set(true);
        });
        t.start();
        try {
            latch.await(); // Wait for the thread to start
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (flag.get()) {
            throw new ExpectedException("Spurious wakeup bug found");
        }
        LockSupport.unpark(t);
    }
}
