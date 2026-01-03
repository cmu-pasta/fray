package org.pastalab.fray.test.core.success.rwlock;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantReadWriteLockDowngradingNoDeadlock {
  public static void main(String[] args) throws InterruptedException {
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    lock.writeLock().lock();
    lock.readLock().lock();
    lock.writeLock().unlock();
    Thread t =
        new Thread(
            () -> {
              lock.readLock().lock();
            });
    t.start();
    t.join();
  }
}
