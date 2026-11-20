package org.pastalab.fray.test.time.success;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Timed waiters on their own monitors while the main thread keeps Fray scheduling. A timed wait
 * that Fray has blocked must not be returned by the JVM on its own timeout: a waiter that leaves
 * wait0 while Fray is scheduling it can run user code holding its monitor, and Fray then blocks on
 * that monitor to wake it (a deadlock), or the waiter finds a pending operation Fray has not
 * converted yet (ClassCastException in objectWaitDoneImpl).
 */
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
