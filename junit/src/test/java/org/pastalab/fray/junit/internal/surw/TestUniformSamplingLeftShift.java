package org.pastalab.fray.junit.internal.surw;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.core.scheduler.SURWScheduler;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

@ExtendWith(FrayTestExtension.class)
public class TestUniformSamplingLeftShift {
    private static CyclicBarrier barrier;
    private static volatile int x = 0;
    public static Map<Integer, Integer> xValues = new HashMap<>();

    private static class Thread1 implements Runnable {
        @Override
        public void run() {
            try {
                barrier.await();
                x = (x << 1);
                x = (x << 1);
                x = (x << 1);
                x = (x << 1);
                x = (x << 1);
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

    private static class Thread2 implements Runnable {
        @Override
        public void run() {
            try {
                barrier.await();
                x = (x << 1) + 1;
                x = (x << 1) + 1;
                x = (x << 1) + 1;
                x = (x << 1) + 1;
                x = (x << 1) + 1;
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

    @ConcurrencyTest(
            scheduler = SURWScheduler.class,
            iterations = 1000
    )
    public void testUniformSampling() {
        Thread[] threads = new Thread[2];
        barrier = new CyclicBarrier(3);
        x = 0;
        threads[0] = new Thread(new Thread1());
        threads[1] = new Thread(new Thread2());

        threads[0].start();
        threads[1].start();
        try {
            barrier.await();
            int value = x;
            xValues.put(value, xValues.getOrDefault(value, 0) + 1);
            for (Thread thread : threads) {
                thread.join();
            }

        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

}
