package org.pastalab.fray.test.core.success.wait;

/** wait(long, int) with nobody to notify returns on its timeout. */
public class TimedWaitMillisNanosNoDeadlock {
  public static void main(String[] args) throws InterruptedException {
    Object o = new Object();
    synchronized (o) {
      o.wait(1000, 1);
    }
  }
}
