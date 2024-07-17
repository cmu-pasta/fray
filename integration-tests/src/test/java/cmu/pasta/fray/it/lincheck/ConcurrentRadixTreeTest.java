package cmu.pasta.fray.it.lincheck;

import cmu.pasta.fray.core.scheduler.POSScheduler;
import cmu.pasta.fray.it.IntegrationTestRunner;
import com.googlecode.concurrenttrees.common.KeyValuePair;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcurrentRadixTreeTest extends IntegrationTestRunner {
    static boolean aaInserted;
    static boolean abInserted;

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
        ConcurrentRadixTree<Integer> map = new ConcurrentRadixTree<>(new DefaultCharArrayNodeFactory());
        aaInserted = false;
        abInserted = false;

        map.put("aaa", 2);
        Thread t1 = new Thread(() -> {
            map.put("aba", -6);
            Iterable<KeyValuePair<Integer>> result = map.getKeyValuePairsForKeysStartingWith("");
            for (KeyValuePair<Integer> p: result) {
                if (p.getKey().equals("aa")) {
                    aaInserted = true;
                }
                if (p.getKey().equals("ab")) {
                    abInserted = true;
                }
            }
        });

        Thread t2 = new Thread(() -> {
            map.put("ab", 4);
            map.put("aa", 5);
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertFalse(aaInserted && !abInserted);
    }
}
