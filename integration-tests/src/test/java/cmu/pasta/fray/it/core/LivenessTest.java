package cmu.pasta.fray.it.core;

import cmu.edu.pasta.fray.junit.annotations.Analyze;
import cmu.edu.pasta.fray.junit.annotations.FrayTest;
import cmu.pasta.fray.core.scheduler.PCTScheduler;
import cmu.pasta.fray.runtime.DeadlockException;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@FrayTest
public class LivenessTest {
    static int i = 0;

    @Analyze(
            scheduler = PCTScheduler.class,
            iteration = 10000,
            expectedException = DeadlockException.class
    )
    public void testLivenessImpl() throws InterruptedException {
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
