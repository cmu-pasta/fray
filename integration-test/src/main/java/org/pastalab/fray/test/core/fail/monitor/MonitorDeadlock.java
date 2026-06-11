package org.pastalab.fray.test.core.fail.monitor;

public class MonitorDeadlock {

  public static void main(String[] args) {
    Object o1 = new Object();
    Object o2 = new Object();
    Thread t1 =
        new Thread(
            () -> {
              synchronized (o1) {
                synchronized (o2) {
                  try {
                    o1.wait();
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                }
              }
            });
    Thread t2 =
        new Thread(
            () -> {
              synchronized (o2) {
                synchronized (o1) {
                  try {
                    o2.wait();
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                }
              }
            });
    t1.start();
    t2.start();
  }
}
