package org.pastalab.fray.test.core.fail.rwlock;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantReadWriteLockDeadlock {
  public static void main(String[] args) throws InterruptedException {
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    lock.writeLock().lock();
    Thread t =
        new Thread(
            () -> {
              lock.readLock().lock();
            });
    t.start();
    t.join();
  }
}
