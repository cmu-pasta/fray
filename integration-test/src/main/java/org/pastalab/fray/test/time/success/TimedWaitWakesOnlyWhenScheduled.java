package org.pastalab.fray.test.time.success;

import java.util.concurrent.atomic.AtomicInteger;

public class TimedWaitWakesOnlyWhenScheduled {
  private static volatile boolean done;

  public static void main(String[] args) throws InterruptedException {
    done = false;
    Thread[] waiters = new Thread[16];
    for (int i = 0; i < waiters.length; i++) {
      waiters[i] =
          new Thread(
              () -> {
                Object o = new Object();
                synchronized (o) {
                  while (!done) {
                    try {
                      o.wait(1);
                    } catch (InterruptedException e) {
                      return;
                    }
                  }
                }
              });
      waiters[i].start();
    }
    AtomicInteger work = new AtomicInteger();
    for (int k = 0; k < 20_000; k++) {
      work.incrementAndGet();
    }
    done = true;
    for (Thread w : waiters) {
      w.join();
    }
  }
}
