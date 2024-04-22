package cmu.pasta.sfuzz.it.core;

import cmu.pasta.sfuzz.it.IntegrationTestRunner;
import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class LivenessTest extends IntegrationTestRunner {
    static int i = 0;

    @Test
    public void testLiveness() {
        String result = runTest(() -> {
            try {
                testLivenessImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }


    public static void testLivenessImpl() throws InterruptedException {
        Thread t = new Thread(() -> {
            ReentrantLock l = new ReentrantLock();
            Condition c = l.newCondition();
            l.lock();
            while (i == 0) {
                try {
                    c.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            l.unlock();
        });
        Thread t2 = new Thread(() -> {
            ReentrantLock l = new ReentrantLock();
            Condition c = l.newCondition();
            l.lock();
            while (i == 0) {
                try {
                    c.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            l.unlock();
        });
        t.start();
        t2.start();
        t.join();
        t2.join();
    }
}
