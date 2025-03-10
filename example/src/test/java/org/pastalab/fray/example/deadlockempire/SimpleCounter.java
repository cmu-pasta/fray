package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

/**
 * Translation of "Simple Counter" from The Deadlock Empire
 *
 * This demonstrates how two threads can both enter a critical section when using
 * simple counter checks as guards.
 *
 * Story: The Parallel Wizard has unleashed the first Dragons upon you - these are
 * terrifying creatures but for some reason, these two dragons appear to have
 * critical weakspots specifically designed to be weak.
 *
 * WIN CONDITION: Get both threads to enter the critical section simultaneously.
 */
@ExtendWith(FrayTestExtension.class)
public class SimpleCounter extends DeadlockEmpireTestBase {
    // Shared variable between threads
    private volatile int counter = 0;

    @ConcurrencyTest
    public void runTest() {
        Thread fiveHeadedDragon = new Thread(() -> {
            while (true) {
                // Expand counter++ to show non-atomic nature
                int tmp = counter + 1;
                counter = tmp;

                if (counter == 5) {
                    criticalSection();
                }
            }
        }, "Five-Headed Dragon");

        Thread threeHeadedDragon = new Thread(() -> {
            while (true) {
                // Expand counter++ to show non-atomic nature
                int tmp = counter + 1;
                counter = tmp;

                if (counter == 3) {
                    criticalSection();
                }
            }
        }, "Three-Headed Dragon");

        fiveHeadedDragon.start();
        threeHeadedDragon.start();
    }
}
