package org.pastalab.fray.benchmark.workload;

/**
 * Benchmark workload that stresses volatile field access scheduling. Multiple threads perform
 * volatile reads and writes to shared state, exercising Fray's memory operation scheduling.
 */
public class VolatileContention {

  private static volatile int sharedCounter = 0;

  public static void run(String[] args) {
    int threadCount = Integer.parseInt(args[0]);
    sharedCounter = 0;
    Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      threads[i] =
          new Thread(
              () -> {
                int local = sharedCounter;
                sharedCounter = local + 1;
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
