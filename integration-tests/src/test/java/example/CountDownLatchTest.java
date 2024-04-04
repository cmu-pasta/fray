package example;

import jdk.jshell.execution.Util;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchTest {
    public static void testCountDown() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(4);
        Worker first = new Worker(1000, latch,
                "WORKER-1");
        Worker second = new Worker(2000, latch,
                "WORKER-2");
        Worker third = new Worker(3000, latch,
                "WORKER-3");
        Worker fourth = new Worker(4000, latch,
                "WORKER-4");
        first.start();
        second.start();
        third.start();
        fourth.start();
        latch.await();
        Utils.log(Thread.currentThread().getName() +
                " has finished");
    }

    public static void testCountDownAwaitInterrupt() throws InterruptedException {
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

    public static void testAwaitAfterCountDown() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        latch.countDown();
        latch.await();
        Utils.log(Thread.currentThread().getName() +
                " has finished");
    }
}

// A class to represent threads for which
// the main thread waits.
class Worker extends Thread {
    private int delay;
    private CountDownLatch latch;

    public Worker(int delay, CountDownLatch latch,
                  String name) {
        super(name);
        this.delay = delay;
        this.latch = latch;
    }

    @Override
    public void run() {
        latch.countDown();
        Utils.log(Thread.currentThread().getName()
                + " finished");
    }
}
