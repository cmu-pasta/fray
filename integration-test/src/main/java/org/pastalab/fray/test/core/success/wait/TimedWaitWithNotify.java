package org.pastalab.fray.test.core.success.wait;

/** A timed wait is still released by a notify, with the timeout far away. */
public class TimedWaitWithNotify {
  private static boolean notified;

  public static void main(String[] args) throws InterruptedException {
    Object o = new Object();
    Thread notifier =
        new Thread(
            () -> {
              synchronized (o) {
                notified = true;
                o.notifyAll();
              }
            });
    synchronized (o) {
      notifier.start();
      while (!notified) {
        o.wait(60_000);
      }
    }
    notifier.join();
  }
}
