package cmu.pasta.fray.it.core;

import cmu.pasta.fray.it.IntegrationTestRunner;
import cmu.pasta.fray.it.Utils;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Semaphore;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
public class SemaphoreTest extends IntegrationTestRunner {
    public static void testSemaphoreImpl() throws InterruptedException {
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

    public static void testInterruptBeforeAcquireImpl() throws InterruptedException {
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

    public static void testInterruptAfterAcquireImpl() throws InterruptedException {
        Semaphore sem = new Semaphore(0);
        ThreadInterrupt mt1 = new ThreadInterrupt(sem);
        mt1.start();
        sem.acquire();
        Utils.log("Interrupting the thread.");
        mt1.interrupt();
        mt1.join();
    }

    @Test
    public void testSemaphore() {
        String event = "[1]: Starting A\n" + "[1]: A is waiting for a permit.\n" + "[1]: A gets a permit.\n" + "[1]: A: 1\n" + "[1]: A: 2\n" + "[1]: A: 3\n" + "[1]: A: 4\n" + "[1]: A: 5\n" + "[1]: A releases the permit.\n" + "[2]: Starting B\n" + "[2]: B is waiting for a permit.\n" + "[2]: B gets a permit.\n" + "[2]: B: 4\n" + "[2]: B: 3\n" + "[2]: B: 2\n" + "[2]: B: 1\n" + "[2]: B: 0\n" + "[2]: B releases the permit.\n" + "[0]: count: 0\n";
        assertEquals(event, runTest(() -> {
            try {
                testSemaphoreImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }));
    }

    @Test
    public void testInterruptBeforeAcquire() {
        String event = "[1]: Starting A\n" + "[1]: A is waiting for a permit.\n" + "[1]: java.lang.InterruptedException\n" + "[1]: A releases the permit.\n" + "[0]: count: 0\n";
        String s = runTest(() -> {
            try {
                testInterruptBeforeAcquireImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
        assertEquals(event, s);
    }

    @Test
    public void testInterruptAfterAcquire() {
        String event = "[1]: Thread is waiting for a permit.\n" + "[0]: Interrupting the thread.\n" + "[1]: java.lang.InterruptedException\n" + "[1]: Thread exits.\n";
        String s = runTest(() -> {
            try {
                testInterruptAfterAcquireImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
        assertEquals(event, s);
    }
}
