package org.pastalab.fray.test.core.success.condition;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionAwaitTimeoutNotifyInterrupt {
  public static void main(String[] args) {
    Lock l = new ReentrantLock();
    Condition c = l.newCondition();
    AtomicBoolean flag = new AtomicBoolean(false);
    Thread t =
        new Thread(
            () -> {
              l.lock();
              try {
                c.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
              } catch (InterruptedException e) {
                flag.set(true);
              }
              l.unlock();
            });
    t.start();
    //        Thread.yield();
    l.lock();
    //        c.signal();
    l.unlock();
    //        Thread.yield();
    t.interrupt();
    l.lock();
    if (!t.isInterrupted()) {
      assert (flag.get());
    }
    l.unlock();
  }
}
