package cmu.pasta.fray.it.lincheck;

import cmu.pasta.fray.core.scheduler.*;
import cmu.pasta.fray.it.IntegrationTestRunner;
import cmu.pasta.fray.it.logicalorderingavl.LogicalOrderingAVL;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LogicalOrderingAVLTest extends IntegrationTestRunner {

    @Test
    public void testConcurrentInsertRemove() {
        String res = runTest(() -> {
            try {
                main(new String[0]);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }, new PCTScheduler(new ControlledRandom(), 3), 50000);
        assertTrue(res.contains("Error found"));
    }

    public static void main(String[] args) throws InterruptedException {
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
