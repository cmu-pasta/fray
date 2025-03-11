package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Translation of "Tutorial 1: Interface" from The Deadlock Empire
 *
 * WIN CONDITION: Get both threads to enter the critical section simultaneously.
 */
@ExtendWith(FrayTestExtension.class)
public class Interface extends DeadlockEmpireTestBase {
    AtomicBoolean flag = new AtomicBoolean(true);

    @ConcurrencyTest
    public void runTest() {
        Thread thread1 = new Thread(() -> {
            criticalSection();
        });
        Thread thread2 = new Thread(() -> {
            if (flag.get()) {
                criticalSection();
            }
        });
        thread1.start();
        thread2.start();
    }
}
