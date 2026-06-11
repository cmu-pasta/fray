package org.pastalab.fray.benchmark.workload;

public class MonitorContention {

  public static void run(String[] args) {
    int threadCount = Integer.parseInt(args[0]);
    Object monitor = new Object();
    int[] counter = new int[] {0};
    Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      threads[i] =
          new Thread(
              () -> {
                synchronized (monitor) {
                  counter[0]++;
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
