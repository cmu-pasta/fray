package org.pastalab.fray.test.core.success.cdl;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchAwaitTimeoutNoDeadlock {
  public static void main(String[] args) throws InterruptedException {
    CountDownLatch cdl = new CountDownLatch(1);
    Thread t =
        new Thread(
            () -> {
              try {
                cdl.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
                cdl.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
                cdl.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            });
    t.start();
    try {
      cdl.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
      cdl.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
      cdl.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
