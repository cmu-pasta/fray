package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

/**
 * Translation of "Confused Counter" from The Deadlock Empire
 *
 * This demonstrates how non-atomic operations can lead to unexpected behaviors
 * when multiple threads are involved.
 *
 * Story: The Parallel Wizard is now more cunning and the dragons he designs and
 * the armies he trains are more resilient than ever.
 *
 * WIN CONDITION: Execute the failure instruction (reach a state that should not happen).
 */
@ExtendWith(FrayTestExtension.class)
public class ConfusedCounter extends DeadlockEmpireTestBase {
    // Shared variables between threads
    private volatile int first = 0;
    private volatile int second = 0;

    @ConcurrencyTest
    public void runTest() {
        Thread thread1 = new Thread(() -> {

            int tmp1 = first + 1;
            first = tmp1;

            int tmp2 = second + 1;
            second = tmp2;

            // Check condition that should never be true
            if (second == 2 && first != 2) {
                failure();
            }
        });

        Thread thread2 = new Thread(() -> {
            int tmp1 = first + 1;
            first = tmp1;

            int tmp2 = second + 1;
            second = tmp2;
        });

        thread1.start();
        thread2.start();
    }
}
