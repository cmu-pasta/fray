package org.pastalab.fray.test.core.success.semaphore;

import static java.util.concurrent.TimeUnit.MICROSECONDS;

import java.util.concurrent.Semaphore;

public class SemaphoreTimeout {
  public static void main(String[] args) throws InterruptedException {
    Semaphore semaphore = new Semaphore(1);
    semaphore.acquire();
    semaphore.tryAcquire(10000000, MICROSECONDS);
  }
}
