package org.pastalab.fray.test.success.rwlock;

public class StaticReentrantReadWriteLockMultipleTests {
    public static void main(String[] args) {
        StaticReentrantReadWriteLockNormal.lock.readLock().lock();
        StaticReentrantReadWriteLockNormal.lock.readLock().unlock();
    }
}
