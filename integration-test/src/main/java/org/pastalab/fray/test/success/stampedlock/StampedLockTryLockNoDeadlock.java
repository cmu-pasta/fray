package org.anonlab.fray.test.success.stampedlock;

import java.util.concurrent.locks.StampedLock;

public class StampedLockTryLockNoDeadlock {

    public static void main(String[] args) throws InterruptedException {
        StampedLock lock = new StampedLock();
        long stamp = lock.writeLock();
        lock.tryReadLock(1000, java.util.concurrent.TimeUnit.MICROSECONDS);
    }
}
