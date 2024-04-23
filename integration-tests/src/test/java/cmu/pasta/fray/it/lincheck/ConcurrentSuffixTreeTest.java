package cmu.pasta.fray.it.lincheck;

import cmu.pasta.fray.core.scheduler.POSScheduler;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.suffix.ConcurrentSuffixTree;
import cmu.pasta.fray.it.IntegrationTestRunner;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcurrentSuffixTreeTest extends IntegrationTestRunner {

    static boolean q1;
    static boolean q2;

    @Test
    public void testConcurrentPutAndGet() {
        String res = runTest(() -> {
            try {
                testConcurrentPutAndGetImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }, new POSScheduler(new Random()), 10000);
        assertTrue(res.contains("Error found"));
    }

    public static void testConcurrentPutAndGetImpl() throws InterruptedException {
        ConcurrentSuffixTree<Integer> tree = new ConcurrentSuffixTree<>(new DefaultCharArrayNodeFactory());
        Thread t1 = new Thread(() -> {
            tree.put("baa", 5);
        });

        Thread t2 = new Thread(() -> {
            q1 = tree.getKeysContaining("baa").iterator().hasNext();
            q2 =  tree.getKeysContaining("aa").iterator().hasNext();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertFalse(q1 && !q2);
    }
}
