package org.pastalab.fray.benchmark.workload;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchBarrier {

  public static void run(String[] args) {
    int threadCount = Integer.parseInt(args[0]);
    CountDownLatch latch = new CountDownLatch(threadCount);
    Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      threads[i] =
          new Thread(
              () -> {
                latch.countDown();
              });
    }
    for (Thread t : threads) {
      t.start();
    }
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    for (Thread t : threads) {
      try {
        t.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
