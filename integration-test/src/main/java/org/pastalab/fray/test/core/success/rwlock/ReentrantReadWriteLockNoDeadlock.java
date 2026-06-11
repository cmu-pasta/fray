package org.pastalab.fray.test.core.success.rwlock;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantReadWriteLockNoDeadlock {
  public static void main(String[] args) throws InterruptedException {
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    lock.readLock().lock();
    Thread t =
        new Thread(
            () -> {
              lock.readLock().lock();
            });
    t.start();
    t.join();
  }
}
