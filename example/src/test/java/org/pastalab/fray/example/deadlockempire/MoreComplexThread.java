package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Translation of "A More Complex Thread" from The Deadlock Empire
 *
 * This demonstrates how recursive lock acquisition and lock releasing patterns can
 * lead to deadlocks and other synchronization issues.
 *
 * Story: You look up the Refactor Lands hill at the lone flag that shows who controls
 * this important territory. You climb fast - you must reach it first. Unfortunately,
 * that won't happen - not one, not two, but three enemy armies are closing in on
 * the hill and they will all reach the flag before you do.
 *
 * WIN CONDITION: Trigger a deadlock between the two threads to stop the enemy armies.
 */
@ExtendWith(FrayTestExtension.class)
public class MoreComplexThread extends DeadlockEmpireTestBase {
    private final ReentrantLock mutex = new ReentrantLock();
    private final ReentrantLock mutex2 = new ReentrantLock();
    private final ReentrantLock mutex3 = new ReentrantLock();
    private volatile boolean flag = false;

    @ConcurrencyTest
    public void runTest() {
        Thread thread1 = new Thread(() -> {
            while (true) {
                // Try to acquire mutex without blocking
                if (mutex.tryLock()) {
                    mutex3.lock();
                    mutex.lock();
                    criticalSection();
                    mutex.unlock();
                    mutex2.lock();
                    flag = false;
                    mutex2.unlock();
                    mutex3.unlock();
                } else {
                    mutex2.lock();
                    flag = true;
                    mutex2.unlock();
                }
            }
        }, "Thread 1");

        Thread thread2 = new Thread(() -> {
            while (true) {
                if (flag) {
                    mutex2.lock();
                    mutex.lock();
                    flag = false;
                    criticalSection();
                    mutex.unlock();
                    mutex2.lock();
                } else {
                    mutex.lock();
                    flag = false;
                    mutex.unlock();
                }
            }
        }, "Thread 2");

        thread1.start();
        thread2.start();
    }
}
