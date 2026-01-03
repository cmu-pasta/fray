package org.pastalab.fray.test.core.success.yield;

import java.util.concurrent.atomic.AtomicBoolean;

public class SpinWaitAndYield {
  public static void main(String[] args) throws InterruptedException {
    var working = new AtomicBoolean(false);
    var t1 =
        new Thread(
            () -> {
              working.set(true);
              Thread.yield();
              working.set(false);
            });
    var t2 =
        new Thread(
            () -> {
              while (working.get()) {
                Thread.yield();
              }
            });
    t1.start();
    t2.start();
    t1.join();
    t2.join();
  }
}
