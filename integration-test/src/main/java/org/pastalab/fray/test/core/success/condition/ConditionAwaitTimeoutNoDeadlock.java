package org.pastalab.fray.test.core.success.condition;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionAwaitTimeoutNoDeadlock {
  public static void main(String[] args) {
    Lock l = new ReentrantLock();
    Condition c = l.newCondition();
    l.lock();
    try {
      c.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
      c.awaitUntil(new java.util.Date(System.currentTimeMillis() + 1000));
      c.awaitNanos(1000000000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      l.unlock();
    }
  }
}
