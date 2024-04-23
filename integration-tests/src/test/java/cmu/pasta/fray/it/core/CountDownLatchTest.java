package cmu.pasta.fray.it.core;

import cmu.pasta.fray.it.IntegrationTestRunner;
import cmu.pasta.fray.it.Utils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.concurrent.CountDownLatch;

public class CountDownLatchTest extends IntegrationTestRunner {

    @Test
    public void testCountDown() {
        String expected = "[1]: WORKER-1 finished\n" + "[2]: WORKER-2 finished\n" + "[3]: WORKER-3 finished\n" + "[4]: WORKER-4 finished\n" + "[0]: Test worker has finished\n";
        assertEquals(expected, runTest(() -> {
            try {
                testCountDownImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }));
    }

    @Test
    public void testAwaitAfterCountDown() {
        String expected = "[0]: Test worker has finished\n";
        assertEquals(expected, runTest(() -> {
            try {
                testAwaitAfterCountDownImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }));
    }

    @Test
    public void testCountDownAwaitInterrupt() {
        String expected = "[1]: Thread has interrupted\n" +
                "[1]: Thread has finished\n";
        assertEquals(expected, runTest(() -> {
            try {
                testCountDownAwaitInterruptImpl();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        }));
    }


    public static void testCountDownImpl() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(4);
        Worker first = new Worker(1000, latch, "WORKER-1");
        Worker second = new Worker(2000, latch, "WORKER-2");
        Worker third = new Worker(3000, latch, "WORKER-3");
        Worker fourth = new Worker(4000, latch, "WORKER-4");
        first.start();
        second.start();
        third.start();
        fourth.start();
        latch.await();
        Utils.log(Thread.currentThread().getName() + " has finished");
    }

    public static void testCountDownAwaitInterruptImpl() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(4);
        Thread t = new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Utils.log("Thread has interrupted");
            }
            Utils.log("Thread has finished");
        });
        t.start();
        t.interrupt();
        t.join();
    }

    public static void testAwaitAfterCountDownImpl() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        latch.countDown();
        latch.await();
        Utils.log(Thread.currentThread().getName() + " has finished");
    }
}

class Worker extends Thread {
    private int delay;
    private CountDownLatch latch;

    public Worker(int delay, CountDownLatch latch, String name) {
        super(name);
        this.delay = delay;
        this.latch = latch;
    }

    @Override
    public void run() {
        latch.countDown();
        Utils.log(Thread.currentThread().getName() + " finished");
    }
}
