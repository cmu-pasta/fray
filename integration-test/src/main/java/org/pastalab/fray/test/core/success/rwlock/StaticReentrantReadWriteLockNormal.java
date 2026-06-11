package org.pastalab.fray.test.core.success.rwlock;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class StaticReentrantReadWriteLockNormal {
  public static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  public static void main(String[] args) throws InterruptedException {
    lock.readLock().lock();
    lock.readLock().unlock();
  }
}
