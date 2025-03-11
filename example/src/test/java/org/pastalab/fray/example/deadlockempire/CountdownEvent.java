package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.CountDownLatch;

/**
 * Translation of "Countdown Event" from The Deadlock Empire
 *
 * This demonstrates how incorrect use of CountDownLatch (Java's equivalent to C#'s CountdownEvent)
 * can lead to deadlocks.
 *
 * Story: The Countdown Dragons are a threat to us - this newest Empire weapon flies
 * right up to you and self-destructs at the worst possible moment, sending ripples
 * of doom throughout our armies.
 *
 * WIN CONDITION: Cause a deadlock by not having enough signals to release all waiting threads.
 */
@ExtendWith(FrayTestExtension.class)
public class CountdownEvent extends DeadlockEmpireTestBase {
    private volatile int progress = 0;
    private final CountDownLatch event = new CountDownLatch(3);

    @ConcurrencyTest
    public void runTest() {
        Thread thread1 = new Thread(() -> {
            progress += 20;

            if (progress >= 20) {
                event.countDown();  // Signal once
            }

            try {
                event.await();  // Wait for event to reach zero
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread 1");

        Thread thread2 = new Thread(() -> {
            progress += 30;

            if (progress >= 30) {
                event.countDown();  // Signal once
            }

            progress += 50;

            if (progress >= 80) {
                event.countDown();  // Signal once more if progress is high enough
            }

            try {
                event.await();  // Wait for event to reach zero
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread 2");

        thread1.start();
        thread2.start();
    }
}
