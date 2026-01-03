package org.pastalab.fray.test.core.success.cdl;

import java.util.concurrent.TimeUnit;

public class CountDownLatchCountDownBeforeAwait {
  public static void main(String[] args) throws InterruptedException {
    java.util.concurrent.CountDownLatch cdl = new java.util.concurrent.CountDownLatch(1);

    cdl.countDown();
    cdl.await(1000, TimeUnit.MICROSECONDS);
  }
}
