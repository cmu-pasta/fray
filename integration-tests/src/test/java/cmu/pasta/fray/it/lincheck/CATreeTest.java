package cmu.pasta.fray.it.lincheck;

import cmu.pasta.fray.core.scheduler.POSScheduler;
import cmu.pasta.fray.it.IntegrationTestRunner;
import cmu.pasta.fray.it.catreemapavl.CATreeMapAVL;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CATreeTest extends IntegrationTestRunner {

    @Test
    public void testClearAndPut() {
        String res = runTest(() -> {
            try {
                testClearAndPutImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }, new POSScheduler(new Random()), 10000);
        assertTrue(res.contains("Error found"));
    }


    public static void testClearAndPutImpl() throws InterruptedException {
        CATreeMapAVL<Long, Integer> map = new CATreeMapAVL<>();
        Thread t1 = new Thread(() -> {
            map.clear();
            map.put(3L, 2);
        });
        Thread t2 = new Thread(() -> {
            map.clear();
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
