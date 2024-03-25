package example;

import cmu.pasta.sfuzz.core.GlobalContext;

import java.util.concurrent.locks.ReentrantReadWriteLock;

// Class 1
// Main class
public class ReentrantReadWriteLockTest {

    private static final ReentrantReadWriteLock lock
            = new ReentrantReadWriteLock(true);

    private static String message = "a";

    public static void test()
            throws InterruptedException
    {

        // Creating threads
        Thread t1 = new Thread(new Read());
        Thread t2 = new Thread(new WriteA());
        Thread t3 = new Thread(new WriteB());

        // Starting threads with help of start() method
        t1.start();
        t2.start();
        t3.start();
        t1.join();
        t2.join();
        t3.join();
    }

    static class Read implements Runnable {

        public void run()
        {

            for (int i = 0; i <= 10; i++) {
                if (lock.isWriteLocked()) {
                    Utils.log(
                            "I'll take the lock from Write");
                }

                // operating lock()
                lock.readLock().lock();

                Utils.log(
                        "ReadThread "
                                + Thread.currentThread().getId()
                                + "Message is " + message);
                lock.readLock().unlock();
            }
        }
    }

    static class WriteA implements Runnable {

        public void run()
        {

            for (int i = 0; i <= 10; i++) {

                // Try block to check fr exceptions
                try {
                    lock.writeLock().lock();
                    message = message.concat("a");
                }
                finally {
                    lock.writeLock().unlock();
                }
            }
        }
    }

    static class WriteB implements Runnable {

        public void run()
        {

            for (int i = 0; i <= 10; i++) {

                // Try block to check for exceptions
                try {
                    lock.writeLock().lock();
                    message = message.concat("b");
                }
                finally {
                    lock.writeLock().unlock();
                }
            }
        }
    }
}

