package org.pastalab.fray.test.success.rwlock;

public class StaticReentrantReadWriteLockMultipleTests {
    public static void main(String[] args) {
        org.pastalab.fray.test.success.rwlock.StaticReentrantReadWriteLockNormal.lock.readLock().lock();
        org.pastalab.fray.test.success.rwlock.StaticReentrantReadWriteLockNormal.lock.readLock().unlock();
    }
}
