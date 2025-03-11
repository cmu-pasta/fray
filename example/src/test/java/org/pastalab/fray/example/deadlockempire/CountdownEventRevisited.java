package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Translation of "Countdown Event Revisited" from The Deadlock Empire
 *
 * This demonstrates another issue with CountDownLatch - attempting to signal it
 * more times than it was initialized with will cause an exception.
 *
 * Story: The Empire is desperate. They cannot think that Countdown Dragons
 * have any future at this point. We've defeated them before, we'll defeat them again.
 *
 * WIN CONDITION: Cause an IllegalStateException by signaling the CountDownLatch too many times.
 */
@ExtendWith(FrayTestExtension.class)
public class CountdownEventRevisited extends DeadlockEmpireTestBase {
    private volatile int progress = 0;
    private final CountDownLatch event = new CountDownLatch(3);

    @ConcurrencyTest
    public void runTest() {
        Thread thread1 = new Thread(() -> {
            while (true) {
                progress += 20;

                event.countDown(); // Signal once

                try {
                    event.await(); // Wait for event to reach zero
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if (progress == 100) {
                    // Game over - we won!
                    return;
                }
            }
        }, "Thread 1");

        Thread thread2 = new Thread(() -> {
            while (true) {
                progress += 30;

                event.countDown(); // Signal once

                progress += 50;

                event.countDown(); // Signal again - potential for signaling too many times

                try {
                    event.await(); // Wait for event to reach zero
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if (progress == 100) {
                    // Game over - we won!
                    return;
                }
            }
        }, "Thread 2");

        thread1.start();
        thread2.start();
    }
}
