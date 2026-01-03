package org.pastalab.fray.test.core.success.condition;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionAwaitStaticField {
  public static ReentrantLock lock = new ReentrantLock();
  public static Condition condition = lock.newCondition();

  public static void main(String[] args) {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    new Thread(
            () -> {
              lock.lock();
              try {
                countDownLatch.countDown();
                condition.await();
              } catch (InterruptedException e) {
              }
              lock.unlock();
            })
        .start();
    new Thread(
            () -> {
              try {
                countDownLatch.await();
              } catch (InterruptedException e) {
              }
              lock.lock();
              condition.signalAll();
              lock.unlock();
            })
        .start();
  }
}
