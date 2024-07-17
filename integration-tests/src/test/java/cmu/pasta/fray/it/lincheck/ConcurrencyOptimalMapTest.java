package cmu.pasta.fray.it.lincheck;

import cmu.pasta.fray.core.scheduler.POSScheduler;
import cmu.pasta.fray.it.IntegrationTestRunner;
import cmu.pasta.fray.it.concurrencyoptimaltreemap.ConcurrencyOptimalTreeMap;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcurrencyOptimalMapTest extends IntegrationTestRunner {

    @Test
    public void testConcurrentPut() {
        String res = runTest(() -> {
            try {
                main(new String[0]);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }, new POSScheduler(new Random()), 10000);
        assertTrue(res.contains("Error found"));
    }


    public static void main(String[] args) throws InterruptedException {
        ConcurrencyOptimalTreeMap<Integer, Integer> map = new ConcurrencyOptimalTreeMap<>();

        Thread t1 = new Thread(() -> {
            map.putIfAbsent(1, 5);
        });

        Thread t2 = new Thread(() -> {
            map.putIfAbsent(3, 1);
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
