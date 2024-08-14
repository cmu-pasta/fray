package org.pastalab.fray.core.test.primitives;

import org.junit.jupiter.api.Test;
import org.pastalab.fray.core.test.FrayRunner;
import org.pastalab.fray.runtime.DeadlockException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WaitTest extends FrayRunner {

    @Test
    public void testWaitWithoutMonitorLock() {
        Throwable result = runTest(() -> {
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
    public void testWaitWithMonitorLock() {
        Throwable result = runTest(() -> {
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
}