package org.pastalab.fray.test.success.rwlock;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantReadWriteLockNormalLockUnlock {
    public static void main(String[] args) throws InterruptedException {
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        AtomicInteger atomicInteger = new AtomicInteger(0);
        Thread t1 = new Thread(() -> {
            rwLock.readLock().lock();
            atomicInteger.incrementAndGet();
            rwLock.readLock().unlock();
        });
        t1.start();
        rwLock.writeLock().lock();
        atomicInteger.incrementAndGet();
        rwLock.writeLock().unlock();
        t1.join();
        rwLock.writeLock().lock();
        atomicInteger.incrementAndGet();
        rwLock.writeLock().unlock();
        rwLock.writeLock().lock();
    }
}
