package org.pastalab.fray.test.core.success.rwlock;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantReadWriteLockMultipleThreadUnlock {
  public static void main(String[] args) throws InterruptedException {
    ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    AtomicInteger atomicInteger = new AtomicInteger(0);

    for (int i = 0; i < 10; i++) {
      Thread t1 =
          new Thread(
              () -> {
                rwLock.readLock().lock();
                atomicInteger.incrementAndGet();
                rwLock.readLock().unlock();
              });
      t1.start();
    }

    for (int i = 0; i < 10; i++) {
      Thread t1 =
          new Thread(
              () -> {
                rwLock.writeLock().lock();
                atomicInteger.incrementAndGet();
                rwLock.writeLock().unlock();
              });
      t1.start();
    }

    rwLock.writeLock().lock();
    atomicInteger.incrementAndGet();
    rwLock.writeLock().unlock();
    rwLock.writeLock().lock();
    atomicInteger.incrementAndGet();
    rwLock.writeLock().unlock();
  }
}
