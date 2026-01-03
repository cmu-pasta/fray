package org.pastalab.fray.test.core.success.park;

import java.util.concurrent.locks.LockSupport;

public class ParkTimeoutInterrupt {
  public static void main(String[] args) {
    Thread t =
        new Thread(
            () -> {
              LockSupport.parkNanos(1000000000);
            });
    t.start();
    t.interrupt();
  }
}
