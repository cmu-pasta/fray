package org.pastalab.fray.core.test.primitives;

import org.junit.jupiter.api.Test;
import org.pastalab.fray.core.test.FrayRunner;
import org.pastalab.fray.runtime.DeadlockException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(result instanceof DeadlockException);
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
}
