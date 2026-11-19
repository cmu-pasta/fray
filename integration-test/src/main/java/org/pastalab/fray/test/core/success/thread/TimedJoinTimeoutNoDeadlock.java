package org.pastalab.fray.test.core.success.thread;

import java.util.concurrent.CountDownLatch;

/** join(long) on a thread that cannot finish yet returns on its timeout. */
public class TimedJoinTimeoutNoDeadlock {
  public static void main(String[] args) throws InterruptedException {
    CountDownLatch release = new CountDownLatch(1);
    Thread worker =
        new Thread(
            () -> {
              try {
                release.await();
              } catch (InterruptedException e) {
                throw new IllegalStateException(e);
              }
            });
    worker.start();
    // Short: the JDK's join loop re-waits until this much REAL time has passed.
    worker.join(10);
    assert worker.isAlive();
    release.countDown();
    worker.join();
  }
}
