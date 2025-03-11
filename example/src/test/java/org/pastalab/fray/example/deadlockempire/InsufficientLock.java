package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

/**
 * Translation of "Insufficient Lock" from The Deadlock Empire
 *
 * This demonstrates that locks don't solve all concurrency issues.
 *
 * Story: The Deadlock Empire strikes again, and in force! Their dragons still have
 * critical sections where they are weak, but this time, they have brought armored
 * locks to hide them from us.
 *
 * WIN CONDITION: Get both threads to fail (or reach the failure instruction).
 */
@ExtendWith(FrayTestExtension.class)
public class InsufficientLock extends DeadlockEmpireTestBase {
    // Shared variables between threads
    private final Object mutex = new Object();
    private int i = 0;

    @ConcurrencyTest
    public void runTest() {
        Thread thread1 = new Thread(() -> {
            while (true) {
                synchronized (mutex) {
                    i += 2;
                    criticalSection();
                    if (i == 5) {
                        failure();
                    }
                }
            }
        });

        Thread thread2 = new Thread(() -> {
            while (true) {
                synchronized (mutex) {
                    i -= 1;
                    criticalSection();
                }
            }
        });

        thread1.start();
        thread2.start();
    }
}
