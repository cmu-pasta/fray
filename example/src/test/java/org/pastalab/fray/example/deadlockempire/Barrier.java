package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Translation of "The Barrier" from The Deadlock Empire
 *
 * This demonstrates the use of CyclicBarrier and how it can be exploited if
 * the number of expected participants is incorrectly configured.
 *
 * Story: Soldiers of the Deadlock Empire let out a mighty cheer as a new device
 * rolls out from their factories. It is a giant armored wall, covered in spikes
 * and it is now rolling on its mighty wheels towards your troops, casting fireballs
 * from its magical engines.
 *
 * WIN CONDITION: Trigger the failure condition by having the fireballCharge be less than 2.
 */
@ExtendWith(FrayTestExtension.class)
public class Barrier extends DeadlockEmpireTestBase {
    private final AtomicInteger fireballCharge = new AtomicInteger(0);

    // We create a barrier with 2 participants, but we have 3 threads
    private final CyclicBarrier barrier = new CyclicBarrier(2);

    @ConcurrencyTest
    public void runTest() {
        Thread thread1 = new Thread(() -> {
            while (true) {
                fireballCharge.incrementAndGet();

                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                }

                if (fireballCharge.get() < 2) {
                    failure();
                }
            }
        }, "Fire Controller");

        Thread thread2 = new Thread(() -> {
            while (true) {
                fireballCharge.incrementAndGet();

                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Charging Module");

        Thread thread3 = new Thread(() -> {
            while (true) {
                fireballCharge.incrementAndGet();

                try {
                    barrier.await();
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                }

                // Reset the charge
                fireballCharge.set(0);
            }
        }, "Reset Module");

        thread1.start();
        thread2.start();
        thread3.start();
    }
}
