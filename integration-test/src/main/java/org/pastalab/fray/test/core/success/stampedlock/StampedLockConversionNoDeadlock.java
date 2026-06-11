package org.pastalab.fray.test.core.success.stampedlock;

import java.util.concurrent.locks.StampedLock;

public class StampedLockConversionNoDeadlock {
  public static void main(String[] args) {
    StampedLock lock = new StampedLock();
    long stamp = lock.writeLock();
    lock.tryConvertToReadLock(stamp);
    lock.readLock();
    lock.readLock();
  }
}
