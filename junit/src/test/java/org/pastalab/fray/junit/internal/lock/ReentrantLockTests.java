package org.pastalab.fray.junit.internal.lock;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.FrayTest;

@ExtendWith(FrayTestExtension.class)
public class ReentrantLockTests {

  @FrayTest
  void testReentrantLockUnlockWithoutLock() {
    ReentrantLock lock = new ReentrantLock();
    boolean assertionTriggered = false;
    try {
      lock.unlock();
    } catch (IllegalMonitorStateException e) {
      assertionTriggered = true;
    }
    assertTrue(assertionTriggered, "Expected IllegalMonitorStateException was not thrown.");
  }

  @FrayTest
  void testReadLockUnlockWithoutLock() {
    ReadWriteLock lock = new ReentrantReadWriteLock();
    boolean assertionTriggered = false;
    try {
      lock.readLock().unlock();
    } catch (IllegalMonitorStateException e) {
      assertionTriggered = true;
    }
    assertTrue(assertionTriggered, "Expected IllegalMonitorStateException was not thrown.");
  }

  @FrayTest
  void testWriteLockUnlockWithoutLock() {
    ReadWriteLock lock = new ReentrantReadWriteLock();
    boolean assertionTriggered = false;
    try {
      lock.writeLock().unlock();
    } catch (IllegalMonitorStateException e) {
      assertionTriggered = true;
    }
    assertTrue(assertionTriggered, "Expected IllegalMonitorStateException was not thrown.");
  }
}
