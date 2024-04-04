package example;

// java program to demonstrate
// use of semaphores Locks

import jdk.jshell.execution.Util;

import java.util.concurrent.*;

//A shared resource/class.
class Shared {
    static int count = 0;
}

class MyThread extends Thread {
    Semaphore sem;
    String threadName;

    public MyThread(Semaphore sem, String threadName) {
        super(threadName);
        this.sem = sem;
        this.threadName = threadName;
    }

    @Override
    public void run() {

        // run by thread A
        if (this.getName().equals("A")) {
            Utils.log("Starting " + threadName);
            try {
                // First, get a permit.
                Utils.log(threadName + " is waiting for a permit.");

                // acquiring the lock
                sem.acquire();

                Utils.log(threadName + " gets a permit.");

                for (int i = 0; i < 5; i++) {
                    Shared.count++;
                    Utils.log(threadName + ": " + Shared.count);
                }
            } catch (InterruptedException exc) {
                Utils.log(exc.toString());
            }

            // Release the permit.
            Utils.log(threadName + " releases the permit.");
            sem.release();
        }

        // run by thread B
        else {
            Utils.log("Starting " + threadName);
            try {
                // First, get a permit.
                Utils.log(threadName + " is waiting for a permit.");

                // acquiring the lock
                sem.acquire();

                Utils.log(threadName + " gets a permit.");
                for (int i = 0; i < 5; i++) {
                    Shared.count--;
                    Utils.log(threadName + ": " + Shared.count);
                }
            } catch (InterruptedException exc) {
                Utils.log(exc.toString());
            }
            // Release the permit.
            Utils.log(threadName + " releases the permit.");
            sem.release();
        }
    }
}

// Driver class
public class SemaphoreTest {
    public static void testSemaphore() throws InterruptedException {
        Shared.count = 0;
        Semaphore sem = new Semaphore(1);
        MyThread mt1 = new MyThread(sem, "A");
        MyThread mt2 = new MyThread(sem, "B");
        mt1.start();
        mt2.start();
        mt1.join();
        mt2.join();
        Utils.log("count: " + Shared.count);
    }

    public static void testInterruptBeforeAcquire() throws InterruptedException {
        Shared.count = 0;
        Semaphore sem = new Semaphore(0);
        MyThread mt1 = new MyThread(sem, "A");
        mt1.start();
        mt1.interrupt();
        mt1.join();
        Utils.log("count: " + Shared.count);
    }


    static class ThreadInterrupt extends Thread {
        Semaphore sem;
        public ThreadInterrupt(Semaphore sem) {
            this.sem = sem;
        }
        @Override
        public void run() {
            try {
                sem.release();
                Utils.log("Thread is waiting for a permit.");
                sem.acquire();
            } catch (InterruptedException exc) {
                Utils.log(exc.toString());
            }
            Utils.log("Thread exits.");
        }
    }

    public static void testInterruptAfterAcquire() throws InterruptedException {
        Semaphore sem = new Semaphore(0);
        ThreadInterrupt mt1 = new ThreadInterrupt(sem);
        mt1.start();
        sem.acquire();
        Utils.log("Interrupting the thread.");
        mt1.interrupt();
        mt1.join();
    }
}
