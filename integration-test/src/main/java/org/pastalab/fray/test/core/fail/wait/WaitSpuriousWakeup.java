package org.pastalab.fray.test.core.fail.wait;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.pastalab.fray.test.ExpectedException;

public class WaitSpuriousWakeup {
  public static void main(String[] args) {
    Object o = new Object();
    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean flag = new AtomicBoolean(false);
    Thread t =
        new Thread(
            () -> {
              synchronized (o) {
                try {
                  latch.countDown();
                  o.wait();
                  flag.set(true);
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
      // Flag can be set before notify is called due to spurious wake up
      if (flag.get()) {
        throw new ExpectedException("Spurious wakeup bug found");
      }
      o.notify();
    }
  }
}
