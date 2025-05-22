package org.pastalab.fray.test.core.fail.stampedlock;

import java.util.concurrent.locks.StampedLock;

public class StampedLockConversionDeadlock {
    public static void main(String[] args) {
        StampedLock lock = new StampedLock();
        long stamp = lock.readLock();
        lock.tryConvertToWriteLock(stamp);
        lock.readLock();
    }
}
