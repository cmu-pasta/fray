package org.pastalab.fray.core.test.primitives;

import org.junit.jupiter.api.Test;
import org.pastalab.fray.core.test.FrayRunner;
import org.pastalab.fray.runtime.DeadlockException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class WaitTest extends FrayRunner {

    @Test
    public void testWaitWithoutMonitorLock() {
        Throwable result = runWithFifo(() -> {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
        assertTrue(result instanceof IllegalMonitorStateException);
    }

    @Test
    public void testWaitWithoutNotify() {
        Throwable result = runWithFifo(() -> {
            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
        assertInstanceOf(DeadlockException.class, result);
    }

    @Test
    public void testWaitWithNotify() {
        Throwable result = runWithFifo(() -> {
            Thread t1 = new Thread(() -> {
                synchronized (this) {
                    notify();
                }
            });
            t1.start();
            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                t1.join();
            } catch (InterruptedException e) {
            }
            return null;
        });
        assertNull(result);
    }

    @Test
    public void testWaitWithNotifyPOS() {
        Throwable result = runWithPOS(() -> {
            Thread t1 = new Thread(() -> {
                synchronized (this) {
                    notify();
                }
            });
            t1.start();
            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                t1.join();
            } catch (InterruptedException e) {
            }
            return null;
        });
        assertInstanceOf(DeadlockException.class, result);
    }

    @Test
    public void testWaitSpuriousWakeUp() {
        Throwable result = runWithPOS(() -> {
            Object o = new Object();
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean flag = new AtomicBoolean(false);
            Thread t = new Thread(() -> {
                synchronized (o) {
                    try {
                        latch.countDown();
                        o.wait();
                        flag.set(true);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            t.start();
            try {
                latch.await(); // Wait for the thread to start
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            synchronized (o) {
                // Flag can be set before notify is called due to spurious wake up
                if (flag.get()) {
                    throw new ExpectedException();
                }
                o.notify();
            }
            return null;
        });
        assertInstanceOf(ExpectedException.class, result);
    }
}
