package cmu.pasta.sfuzz.it.lincheck;
import cmu.pasta.sfuzz.core.scheduler.POSScheduler;
import cmu.pasta.sfuzz.it.IntegrationTestRunner;
import org.jctools.maps.NonBlockingHashMapLong;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class NonBlockingHashMapLongTest extends IntegrationTestRunner {

    @Test
    public void testConcurrentRemoveReplace() {
        String res = runTest(() -> {
            try {
                testConcurrentRemoveReplaceImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }, new POSScheduler(new Random()), 10000);
        assertTrue(res.contains("Error found"));
    }

    public static void testConcurrentRemoveReplaceImpl() throws InterruptedException {
        NonBlockingHashMapLong<Integer> map = new NonBlockingHashMapLong<Integer>();
        map.putIfAbsent(2, 6);

        Thread t1 = new Thread(() -> {
            map.remove(2);
        });

        Thread t2 = new Thread(() -> {
            map.replace(2, 8);
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        assertNotEquals(8, map.get(2));
    }
}
