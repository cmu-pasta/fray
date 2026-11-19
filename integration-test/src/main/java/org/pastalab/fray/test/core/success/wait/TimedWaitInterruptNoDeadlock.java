package org.pastalab.fray.test.core.success.wait;

/** An interrupt ends a timed wait with InterruptedException, wherever it lands. */
public class TimedWaitInterruptNoDeadlock {
  public static void main(String[] args) throws InterruptedException {
    Object o = new Object();
    Thread waiter =
        new Thread(
            () -> {
              synchronized (o) {
                try {
                  while (true) {
                    o.wait(60_000);
                  }
                } catch (InterruptedException expected) {
                  // The only way out.
                }
              }
            });
    waiter.start();
    waiter.interrupt();
    waiter.join();
  }
}
