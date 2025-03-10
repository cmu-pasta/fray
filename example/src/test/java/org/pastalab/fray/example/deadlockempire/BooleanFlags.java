package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

/**
 * Translation of "Boolean Flags Are Enough For Everyone" from The Deadlock Empire
 *
 * This demonstrates how two threads can both enter a critical section when using
 * a simple boolean flag as a mutex.
 *
 * Story: The Deadlock Empire opened its gates and from them surged massive amounts of soldiers,
 * loyal servants of the evil Parallel Wizard. Two armies are approaching our border keeps.
 *
 * WIN CONDITION: Get both threads to enter the critical section simultaneously.
 */
@ExtendWith(FrayTestExtension.class)
public class BooleanFlags extends DeadlockEmpireTestBase {
    // Shared variable between threads
    private volatile boolean flag = false;

    @ConcurrencyTest
    public void runTest() {
        Thread firstArmy = new Thread(() -> {
            while (true) {
                // Guard - wait until flag is false
                while (flag != false) {
                    // Empty statement, just waiting
                }

                // Set flag to signal entering critical section
                flag = true;

                // Enter critical section
                criticalSection();

                // Reset flag when done
                flag = false;
            }
        }, "First Army");

        Thread secondArmy = new Thread(() -> {
            while (true) {
                // Guard - wait until flag is false
                while (flag != false) {
                    // Empty statement, just waiting
                }

                // Set flag to signal entering critical section
                flag = true;

                // Enter critical section
                criticalSection();

                // Reset flag when done
                flag = false;
            }
        }, "Second Army");

        firstArmy.start();
        secondArmy.start();
    }
}
