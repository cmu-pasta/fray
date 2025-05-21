package org.pastalab.fray.test.fail.stampedlock;

import java.util.concurrent.locks.StampedLock;

public class StampedLockDeadlock {
    public static void main(String[] args) {
        StampedLock lock = new StampedLock();
        lock.readLock();
        lock.writeLock();
    }
}
