package cmu.pasta.fray.it.lincheck;

import cmu.pasta.fray.core.scheduler.POSScheduler;
import cmu.pasta.fray.it.IntegrationTestRunner;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcurrentLinkedDequeTest extends IntegrationTestRunner {
    public static int t1Value = 0;
    public static int t2Value = 0;

    @Test
    void testLinearizedExecution() throws IOException {
        String res = runTest(() -> {
            try {
                testLinearizedExecutionImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }, new POSScheduler(new Random()), 10000);
        assertTrue(res.contains("Error found"));
    }

    void testLinearizedExecutionImpl() throws InterruptedException {
        t1Value = 0;
        t2Value = 0;
        ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<>();
        Thread t1 = new Thread(() -> {
            deque.addLast(-6);
            t1Value = deque.peekFirst();
        });
        Thread t2 = new Thread(() -> {
            deque.addFirst(-8);
            t2Value = deque.pollLast();
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertFalse(t1Value == -8 && t2Value == -8);
    }
}