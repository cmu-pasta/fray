package org.pastalab.fray.benchmark.workload;

import java.util.concurrent.ArrayBlockingQueue;

public class ProducerConsumer {

  public static void run(String[] args) {
    int threadCount = Integer.parseInt(args[0]);
    int producers = threadCount / 2;
    int consumers = threadCount - producers;
    ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(threadCount);
    Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < producers; i++) {
      final int item = i;
      threads[i] =
          new Thread(
              () -> {
                try {
                  queue.put(item);
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
              });
    }
    for (int i = 0; i < consumers; i++) {
      threads[producers + i] =
          new Thread(
              () -> {
                try {
                  queue.take();
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
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
