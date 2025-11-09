package org.pastalab.fray.junit.internal.lock;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.locks.StampedLock;

@ExtendWith(FrayTestExtension.class)
public class StampedLockTests {
    @ConcurrencyTest
    public void testStampedLockSimpleReadWrite() throws InterruptedException {
        StampedLock lock = new StampedLock();

        long stamp = lock.readLock();
        Thread t = new Thread(() -> {
            try {
                long writeStamp = lock.writeLock();
                lock.unlockWrite(writeStamp);
            } catch (Exception ex) {}
        });
        Thread t2 = new Thread(() -> {
            try {
                long writeStamp = lock.writeLock();
                lock.unlockWrite(writeStamp);
            } catch (Exception ex) {}
        });
        t.start();
        t2.start();
        long stamp2 = lock.readLock();
        lock.unlock(stamp);
        lock.unlock(stamp2);
        t.join();
        t2.join();
    }

    @ConcurrencyTest
    public void testStampedLockDeadlock() throws InterruptedException {
        StampedLock lock = new StampedLock();

        long stamp = lock.readLock();
        Thread t = new Thread(() -> {
            try {
                long writeStamp = lock.writeLock();
                lock.unlockWrite(writeStamp);
            } catch (Exception ex) {}
        });
        t.start();
        t.join();
    }
}
