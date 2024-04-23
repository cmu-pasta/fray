package cmu.pasta.fray.it.core;

import cmu.pasta.fray.it.IntegrationTestRunner;
import cmu.pasta.fray.it.Utils;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReentrantReadWriteLockTest extends IntegrationTestRunner {

    private static final ReentrantReadWriteLock lock
            = new ReentrantReadWriteLock(true);

    private static String message = "";

    @Test
    public void testReentrantLock() {
        String s = runTest(() -> {
            try {
                testReentrantLockImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
        assertEquals("[3]: ReadThread Message is ab\n", s);
    }

    @Test
    public void testInterrupt() {
        String s = runTest(() -> {
            try {
                testInterruptImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
        assertEquals("[1]: Thread 1 trying to acquire write lock\n" +
                "[1]: Thread 1 interrupted\n", s);
    }

    @Test
    public void testInterruptAfterAcquire() {
        String s = runTest(() -> {
            try {
                testInterruptAfterAcquireImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
        assertEquals("[1]: Thread 1 acquired write lock\n" +
                "[0]: Thread 1 is interrupted\n" +
                "[1]: Thread 1 interrupted\n", s);
    }

    public static void testReentrantLockImpl()
            throws InterruptedException {
        // Creating threads
        message = "";
        Thread t2 = new Thread(new ReentrantReadWriteLockTest.WriteA());
        Thread t3 = new Thread(new ReentrantReadWriteLockTest.WriteB());
        Thread t1 = new Thread(new ReentrantReadWriteLockTest.Read());

        // Starting threads with help of start() method
        t2.start();
        t3.start();
        t1.start();
        t1.join();
        t2.join();
        t3.join();
    }

    public static void testInterruptImpl() throws InterruptedException {
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        Thread t1 = new Thread(() -> {
            try {
                Utils.log("Thread 1 trying to acquire write lock");
                lock.writeLock().lockInterruptibly();
            } catch (InterruptedException e) {
                Utils.log("Thread 1 interrupted");
            }
        });
        lock.writeLock().lock();
        t1.start();
        t1.interrupt();
        t1.join();
        lock.writeLock().unlock();
    }

    public static void testInterruptAfterAcquireImpl() throws InterruptedException {
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        Semaphore semaphore = new Semaphore(0);
        Thread t1 = new Thread(() -> {
            try {
                Utils.log("Thread 1 acquired write lock");
                semaphore.release();
                lock.writeLock().lockInterruptibly();
            } catch (InterruptedException e) {
                Utils.log("Thread 1 interrupted");
            }
        });
        lock.writeLock().lock();
        t1.start();
        semaphore.acquire();
        Utils.log("Thread 1 is interrupted");
        t1.interrupt();
        t1.join();
        lock.writeLock().unlock();
    }

    static class Read implements Runnable {

        public void run() {

            if (lock.isWriteLocked()) {
                Utils.log(
                        "I'll take the lock from Write");
            }

            // operating lock()
            lock.readLock().lock();

            Utils.log(
                    "ReadThread Message is " + message);
            lock.readLock().unlock();
        }
    }

    static class WriteA implements Runnable {

        public void run() {
            // Try block to check fr exceptions
            try {
                lock.writeLock().lock();
                message = message.concat("a");
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    static class WriteB implements Runnable {

        public void run() {
            // Try block to check for exceptions
            try {
                lock.writeLock().lock();
                message = message.concat("b");
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
}

