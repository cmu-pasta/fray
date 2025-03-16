package org.pastalab.fray.junit.internal.surw;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.core.scheduler.RandomScheduler;
import org.pastalab.fray.core.scheduler.SURWScheduler;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

@ExtendWith(FrayTestExtension.class)
public class TestUniformSampling {
    private static CyclicBarrier barrier;
    private static volatile int x = 0;
    public static Map<Integer, Integer> xValues = new HashMap<>();

    private static class Thread1 implements Runnable {
        @Override
        public void run() {
            try {
                barrier.await();
                x = 1;
                x = 2;
                x = 3;
                x = 4;
                x = 5;
                x = 6;
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
                x = 7;
                x = 8;
                x = 9;
                x = 10;
                x = 11;
                x = 12;
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

    private static class Thread3 implements Runnable {
        @Override
        public void run() {
            try {
                barrier.await();
                x = 13;
                x = 14;
                x = 15;
                x = 16;
                x = 17;
                x = 18;
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

    @ConcurrencyTest(
            scheduler = SURWScheduler.class,
            iterations = 5000
    )
    public void testUniformSampling() {
        Thread[] threads = new Thread[3];
        barrier = new CyclicBarrier(4);
        x = 0;
        threads[0] = new Thread(new Thread1());
        threads[1] = new Thread(new Thread2());
        threads[2] = new Thread(new Thread3());

        threads[0].start();
        threads[1].start();
        threads[2].start();
        try {
            barrier.await();
            int value = x;
            for (Thread thread : threads) {
                thread.join();
            }
            xValues.put(value, xValues.getOrDefault(value, 0) + 1);

        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }



}
