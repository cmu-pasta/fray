package cmu.pasta.fray.it.core;


import cmu.pasta.fray.it.IntegrationTestRunner;
import cmu.pasta.fray.it.Utils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadInterruptTest extends IntegrationTestRunner {
    @Test
    public void testInterruptBeforeWait() {
        assertEquals("[1]: Interrupted\n", runTest(() -> {
            try {
                testInterruptBeforeWaitImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }));
    }

    @Test
    public void testInterruptDuringWait() {
        assertEquals("[1]: Interrupted\n", runTest(() -> {
            try {
                testInterruptDuringWaitImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;

        }));
    }

    @Test
    public void testInterruptCleared() {
        runTest(() -> {
            try {
                testInterruptClearedImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }
    public static void testInterruptClearedImpl() throws InterruptedException {
        final Object lock1 = new Object();
        final Object lock2 = new Object();
        final boolean[] t1Waiting = {false};
        Thread t1 = new Thread() {
            @Override
            public void run() {
                try {
                    synchronized (lock1) {
                        assertTrue(Thread.interrupted());
                        t1Waiting[0] = true;
                        lock1.notify();
                        lock1.wait();
                    }
                } catch (InterruptedException e) {
                    fail("No exception should be thrown");
                }

            }
        };
        Thread t2 = new Thread() {
            @Override
            public void run() {
                t1.interrupt();
                synchronized (lock2) {
                    lock2.notify();
                }

                synchronized (lock1) {
                    if (!t1Waiting[0]) {
                        try {
                            lock1.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    lock1.notify();
                }
            }
        };
        t1.start();
        t2.start();
        synchronized (lock1) {
            synchronized (lock2) {
                lock2.wait();
            }
        }
        t1.join();
        t2.join();
    }
    public static void testInterruptBeforeWaitImpl() throws InterruptedException {
        final Object lock1 = new Object();
        final Object lock2 = new Object();
        Thread t1 = new Thread() {
            @Override
            public void run() {
                try {
                    synchronized (lock1) {
                        lock1.wait();
                    }
                } catch (InterruptedException e) {
                    Utils.log("Interrupted");
                }

            }
        };
        Thread t2 = new Thread() {
            @Override
            public void run() {
                t1.interrupt();
                synchronized (lock2) {
                    lock2.notify();
                }
            }
        };
        t1.start();
        t2.start();
        synchronized (lock1) {
            synchronized (lock2) {
                lock2.wait();
            }
        }
        t1.join();
        t2.join();
    }

    public static void testInterruptDuringWaitImpl() throws InterruptedException {
        final Object lock1 = new Object();
        final boolean[] t1Waiting = {false};
        Thread t1 = new Thread() {
            @Override
            public void run() {
                synchronized (lock1) {
                    try {
                        t1Waiting[0] = true;
                        lock1.notify();
                        lock1.wait();
                    } catch (InterruptedException e) {
                        Utils.log("Interrupted");
                    }
                }
            }
        };
        Thread t2 = new Thread() {
            @Override
            public void run() {
                synchronized (lock1) {
                    if (!t1Waiting[0]) {
                        try {
                            lock1.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    t1.interrupt();
                }
            }
        };
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
