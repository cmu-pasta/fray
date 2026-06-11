package org.pastalab.fray.test.core.fail.wait;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.pastalab.fray.test.ExpectedException;

public class RescheduleBeforeWaitReacquireMonitorLock {

  public static void rescheduleBeforeWaitReacquireMonitorLock() throws InterruptedException {
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
    latch.await(); // Wait for the thread to start
    synchronized (o) {
      o.notify();
    }
    Thread.yield();
    synchronized (o) {
      if (!flag.get()) {
        throw new ExpectedException("flag is false");
      }
    }
  }

  public static void main(String[] args) throws InterruptedException {
    rescheduleBeforeWaitReacquireMonitorLock();
  }
}
