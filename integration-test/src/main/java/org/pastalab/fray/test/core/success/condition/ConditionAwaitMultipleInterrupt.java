package org.pastalab.fray.test.core.success.condition;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionAwaitMultipleInterrupt {
  public static void main(String[] args) {
    ReentrantLock lock = new ReentrantLock();
    Condition cond = lock.newCondition();
    for (int i = 0; i < 2; i++) {
      Thread t =
          new Thread(
              () -> {
                lock.lock();
                try {
                  cond.await();
                } catch (InterruptedException e) {
                  // Expected interruption
                } finally {
                  lock.unlock();
                }
              });
      t.start();
      new Thread(
              () -> {
                t.interrupt();
              })
          .start();
    }
    new Thread(
            () -> {
              for (int i = 0; i < 10; i++) {
                lock.lock();
                cond.signal();
                lock.unlock();
                Thread.yield();
              }
            })
        .start();
  }
}
