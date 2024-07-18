package cmu.pasta.fray.it.lincheck;

import cmu.pasta.fray.core.command.Configuration;
import cmu.pasta.fray.core.scheduler.*;
import cmu.pasta.fray.it.IntegrationTestRunner;
import cmu.pasta.fray.it.logicalorderingavl.LogicalOrderingAVL;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
        }, new PCTScheduler(new ControlledRandom(), 3), -1);
        assertTrue(res.contains("Error found"));
    }

    public static void main(String[] args) throws InterruptedException {
        LogicalOrderingAVL<Integer, Integer> map = new LogicalOrderingAVL<>();
        map.putIfAbsent(5, 2);
        map.put(3, 5);

        Thread t1 = new Thread(() -> {
            map.get(4);
            map.put(4, 5);
        });

        Thread t2 = new Thread(() -> {
            map.remove(5);
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }
}
