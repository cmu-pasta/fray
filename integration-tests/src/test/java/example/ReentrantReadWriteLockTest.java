package example;

import jdk.jshell.execution.Util;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantReadWriteLockTest {

    private static final ReentrantReadWriteLock lock
            = new ReentrantReadWriteLock(true);

    private static String message = "";

    public static void test()
            throws InterruptedException {
        // Creating threads
        message = "";
        Thread t2 = new Thread(new WriteA());
        Thread t3 = new Thread(new WriteB());
        Thread t1 = new Thread(new Read());

        // Starting threads with help of start() method
        t2.start();
        t3.start();
        t1.start();
        t1.join();
        t2.join();
        t3.join();
    }

    public static void testInterrupt() throws InterruptedException {
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

    public static void testInterruptAfterAcquire() throws InterruptedException {
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

