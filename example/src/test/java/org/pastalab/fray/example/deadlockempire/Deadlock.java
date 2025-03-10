package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Translation of "Deadlock" from The Deadlock Empire
 *
 * This demonstrates a classic deadlock where two threads each hold one lock
 * and try to acquire the other, creating a circular waiting pattern.
 *
 * Story: The Deadlock Empire is moving a great army to intimidate developers
 * to accept their paradigms. You must stop this from happening.
 *
 * WIN CONDITION: Cause a deadlock to occur.
 */
@ExtendWith(FrayTestExtension.class)
public class Deadlock extends DeadlockEmpireTestBase {
    private final ReentrantLock mutex = new ReentrantLock();
    private final ReentrantLock mutex2 = new ReentrantLock();

    @ConcurrencyTest
    public void runTest() {
        Thread thread1 = new Thread(() -> {
            mutex.lock();
            mutex2.lock();
            criticalSection();
            mutex.unlock();
            mutex2.unlock();
        });

        Thread thread2 = new Thread(() -> {
            mutex2.lock();
            mutex.lock();
            criticalSection();
            mutex2.unlock();
            mutex.unlock();
        });

        thread1.start();
        thread2.start();
    }
}
