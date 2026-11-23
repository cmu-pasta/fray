package org.pastalab.fray.test.time.success;

/** A timed wait that nobody notifies returns on its timeout, in Fray's virtual time. */
public class ObjectWaitTimeCheck {
  public static void main(String[] args) throws InterruptedException {
    // A wait may return early (Fray models spurious wakeups), so wait as a correct caller does:
    // until the deadline. This ends only if Fray delivers the timeouts.
    Object o = new Object();
    long deadline = System.nanoTime() + 1000_000_000L;
    synchronized (o) {
      long remaining;
      while ((remaining = deadline - System.nanoTime()) > 0) {
        o.wait(Math.max(1L, remaining / 1_000_000L));
      }
    }
  }
}
