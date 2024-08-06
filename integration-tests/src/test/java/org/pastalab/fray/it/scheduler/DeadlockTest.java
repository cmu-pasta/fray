package org.pastalab.fray.it.scheduler;
import org.pastalab.fray.core.scheduler.POSScheduler;
import org.pastalab.fray.junit.annotations.Analyze;
import org.pastalab.fray.junit.annotations.FrayTest;
import org.pastalab.fray.runtime.DeadlockException;

import java.util.concurrent.atomic.AtomicInteger;

@FrayTest
public class DeadlockTest {
    private static class FrayExample extends Thread {
        static Object o = new Object();
        static AtomicInteger a = new AtomicInteger();
        static volatile int b;

        public void run() {
            int x = a.getAndIncrement();
            synchronized (o) { // monitorenter
                if (x == 0) {
                    try {
                        o.wait();
                    } catch (InterruptedException ignore) {
                    }
                } else {
                    o.notify();
                }
            } // monitorexit
            b = x;
        }
    }

    @Analyze(
            expectedException = DeadlockException.class,
            scheduler = POSScheduler.class,
            iteration = 1000
    )
    public void testDeadlock() throws Exception {
        FrayExample.a = new AtomicInteger();
        FrayExample.b = 0;
        FrayExample[] threads = {new FrayExample(), new FrayExample()};
        for (var thread : threads) thread.start();
        for (var thread : threads) thread.join();
    }

    @Analyze(
            expectedException = DeadlockException.class,
            replay = "classpath:/org/pastalab/fray/it/scheduler/DeadlockTest"
    )
    public void testDeadlockWithReplay() throws Exception {
        FrayExample.a = new AtomicInteger();
        FrayExample.b = 0;
        FrayExample[] threads = {new FrayExample(), new FrayExample()};
        for (var thread : threads) thread.start();
        for (var thread : threads) thread.join();
    }
}
