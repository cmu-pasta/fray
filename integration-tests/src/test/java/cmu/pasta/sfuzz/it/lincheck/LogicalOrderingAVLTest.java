package cmu.pasta.sfuzz.it.lincheck;

import cmu.pasta.sfuzz.core.scheduler.POSScheduler;
import cmu.pasta.sfuzz.it.IntegrationTestRunner;
import cmu.pasta.sfuzz.it.logicalorderingavl.LogicalOrderingAVL;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LogicalOrderingAVLTest extends IntegrationTestRunner {

    @Test
    public void testConcurrentInsertRemove() {
        String res = runTest(() -> {
            try {
                testConcurrentInsertRemoveImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }, new POSScheduler(new Random()), 10000);
        assertTrue(res.contains("Error found"));
    }

    public static void testConcurrentInsertRemoveImpl() throws InterruptedException {
        LogicalOrderingAVL<Integer, Integer> map = new LogicalOrderingAVL<>();
        map.put(1, 4);

        Thread t1 = new Thread(() -> {
            map.put(2, 6);
        });

        Thread t2 = new Thread(() -> {
            map.putIfAbsent(5, 6);
            map.remove(2);
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }
}
