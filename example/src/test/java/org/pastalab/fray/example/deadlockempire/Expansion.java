package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

/**
 * Translation of "Tutorial 2: Non-Atomic Instructions" from The Deadlock Empire
 *
 * This demonstrates how non-atomic operations like a++ can be interleaved between
 * threads, causing unexpected behavior.
 *
 * WIN CONDITION: Get both threads to enter the critical section simultaneously.
 */
@ExtendWith(FrayTestExtension.class)
public class Expansion extends DeadlockEmpireTestBase {
    // Shared variable between threads, similar to the game's global state
    private volatile int a = 0;

    @ConcurrencyTest
    public void runTest() {
        Thread thread1 = new Thread(() -> {
            // This represents the expanded a = a + 1 operation
            // In reality, this is not atomic and consists of:
            // 1. Read value of a
            int localA1 = a;  // Read a (value: 0)
            // 2. Add 1 to it
            localA1 = localA1 + 1;  // Compute a + 1 (result: 1)
            // 3. Write back to a
            a = localA1;  // Write back (a becomes 1)

            // Check condition and enter critical section
            if (a == 1) {
                criticalSection();
            }
        });

        Thread thread2 = new Thread(() -> {
            // Same operation as in thread1, but potentially interleaved differently
            int localA2 = a;  // This might read a=0 if executed before thread1 writes back
            localA2 = localA2 + 1;
            a = localA2;  // This might make a=1 even if thread1 already set a=1

            // Both threads might enter the critical section if interleaved properly
            if (a == 1) {
                criticalSection();
            }
        });

        thread1.start();
        thread2.start();
    }
}
