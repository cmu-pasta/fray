package org.pastalab.fray.benchmark.workload;

public class ThreadCreation {

  public static void run(String[] args) {
    int threadCount = Integer.parseInt(args[0]);
    Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      threads[i] =
          new Thread(
              () -> {
                Thread.yield();
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
