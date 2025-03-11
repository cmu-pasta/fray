package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.Semaphore;

/**
 * Translation of "Boss Fight" from The Deadlock Empire
 *
 * This is the final challenge of the Deadlock Empire game, requiring mastery
 * of all concurrency concepts to defeat the Parallel Wizard.
 *
 * Story: You defeated the Parallel Wizard's armies in battle and now you finally
 * stand in front of the doors of his massive fortress. The time has come to end
 * this war. The Parallel Wizard keeps his only weakness in his inner sanctum.
 * You reluctantly cast the spell that splits your spirit in two and enter the fortress...
 *
 * WIN CONDITION: Find a way to make both threads enter their critical sections,
 * exploiting complex synchronization issues with condition variables and semaphores.
 */
@ExtendWith(FrayTestExtension.class)
public class BossFight extends DeadlockEmpireTestBase {
    private volatile int darkness = 0;
    private volatile int evil = 0;
    private final Semaphore fortress = new Semaphore(0);
    private final Object sanctum = new Object();

    @ConcurrencyTest
    public void runTest() {
        Thread thread1 = new Thread(() -> {
            while (true) {
                darkness++;
                evil++;

                if (darkness != 2 && evil != 2) {
                    if (fortress.tryAcquire()) {
                        try {
                            fortress.acquire(); // Wait for a permit

                            synchronized (sanctum) {
                                try {
                                    sanctum.wait(); // Wait to be notified
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }

                                criticalSection();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        }, "Master Scheduler (Part 1)");

        Thread thread2 = new Thread(() -> {
            while (true) {
                darkness++;
                evil++;

                if (darkness != 2 && evil == 2) {
                    synchronized (sanctum) {
                        sanctum.notify(); // Wake up waiting thread
                    }

                    criticalSection();
                }

                fortress.release();
                darkness = 0;
                evil = 0;
            }
        }, "Master Scheduler (Part 2)");

        thread1.start();
        thread2.start();
    }
}
