package cmu.pasta.fray.it.lincheck;

import cmu.pasta.fray.core.scheduler.POSScheduler;
import cmu.pasta.fray.it.IntegrationTestRunner;
import cmu.pasta.fray.it.catreemapavl.CATreeMapAVL;
import cmu.pasta.fray.it.semaphore.Semaphore;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractQueueSynchronizerTest extends IntegrationTestRunner {

    @Test
    public void testClearAndPut() {
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

        Semaphore sem = new Semaphore(1, true);
        Thread t1 = new Thread(() -> {
            try {
                sem.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                sem.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                sem.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                sem.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        t1.start();
        t2.start();
        sem.release();
        t1.join();
        t2.join();
    }
}
