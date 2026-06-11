package org.pastalab.fray.test.core.fail.wait;

public class TwoWaitDeadlock {
  public static void main(String[] args) {
    Object lock1 = new Object();
    new Thread(
            () -> {
              synchronized (lock1) {
                try {
                  lock1.wait();
                } catch (InterruptedException e) {
                }
              }
            })
        .start();
    new Thread(
            () -> {
              synchronized (lock1) {
                try {
                  lock1.wait();
                } catch (InterruptedException e) {
                }
              }
            })
        .start();
  }
}
