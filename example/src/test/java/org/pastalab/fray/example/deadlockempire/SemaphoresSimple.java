package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Translation of "Semaphores" from The Deadlock Empire
 *
 * This demonstrates how semaphores can be incorrectly used for synchronization.
 *
 * Story: You behold the Factory Lands of the Deadlock Empire. Everything runs
 * smoothly and efficiently, all factories producing new materials at the same time
 * without unnecessary delays. But there are weaknesses - it may be efficient but
 * it is unstable.
 *
 * WIN CONDITION: Get both threads to enter the critical section simultaneously.
 */
@ExtendWith(FrayTestExtension.class)
public class SemaphoresSimple extends DeadlockEmpireTestBase {
    // Start with 0 permits
    private final Semaphore semaphore = new Semaphore(0);

    @ConcurrencyTest
    public void runTest() {
        Thread thread1 = new Thread(() -> {
            while (true) {
                try {
                    semaphore.acquire();
                    criticalSection();
                    semaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        Thread thread2 = new Thread(() -> {
            while (true) {
                try {
                    if (semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)) {
                        criticalSection();
                        semaphore.release();
                    } else {
                        // Create a new permit if we couldn't get one
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        thread1.start();
        thread2.start();
    }
}
