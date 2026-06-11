package org.pastalab.fray.benchmark.workload;

import java.util.concurrent.locks.ReentrantLock;

public class LockContention {

  public static void run(String[] args) {
    int threadCount = Integer.parseInt(args[0]);
    ReentrantLock lock = new ReentrantLock();
    int[] counter = new int[] {0};
    Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      threads[i] =
          new Thread(
              () -> {
                lock.lock();
                try {
                  counter[0]++;
                } finally {
                  lock.unlock();
                }
              });
    }
    for (Thread t : threads) {
      t.start();
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
