package org.pastalab.fray.test.time.success;

import java.util.concurrent.CountDownLatch;

public class WaitTimeoutNoDeadlock {
  public static void main(String[] args) throws InterruptedException {
    long startTime = System.currentTimeMillis();
    CountDownLatch latch = new CountDownLatch(1);
    latch.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
    long elapsedTime = System.currentTimeMillis() - startTime;
    assert (elapsedTime >= 1000); // Ensure at least 1 second has passed
  }
}
