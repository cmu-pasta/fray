package org.pastalab.fray.test.core.fail.wait;

import org.pastalab.fray.test.ExpectedException;

/** Fray still explores a timed wait returning before its notify (timeout or spurious wakeup). */
public class TimedWaitReturnsWithoutNotify {
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
      o.wait(60_000);
      if (!notified) {
        throw new ExpectedException("Timed wait returned before the notify");
      }
    }
    notifier.join();
  }
}
