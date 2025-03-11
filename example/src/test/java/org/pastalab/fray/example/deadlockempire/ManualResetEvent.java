package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Translation of "Manual Reset Event" from The Deadlock Empire
 *
 * This demonstrates the use of a simple synchronization primitive similar to
 * C#'s ManualResetEventSlim, which blocks until a signal is set.
 *
 * Story: We are pushing the enemy back. The next thing we need to do -
 * we must learn to fight the Deadlock Empire's most modern weaponry.
 *
 * WIN CONDITION: Reach the failure condition.
 */
@ExtendWith(FrayTestExtension.class)
public class ManualResetEvent extends DeadlockEmpireTestBase {
    private volatile int counter = 0;

    // Java doesn't have a direct equivalent to C#'s ManualResetEventSlim
    // so we implement a simple version with a ReentrantLock and Condition
    private final Lock syncLock = new ReentrantLock();
    private final Condition syncCondition = syncLock.newCondition();
    private volatile boolean isSignaled = false;

    @ConcurrencyTest
    public void runTest() {
        Thread waiter = new Thread(() -> {
            while (true) {
                // Wait until the event is signaled
                waitForEvent();

                // Check if counter is odd, which should never happen if the code works correctly
                if (counter % 2 == 1) {
                    failure();
                }
            }
        }, "Waiter");

        Thread signaler = new Thread(() -> {
            while (true) {
                // Reset the event (make threads wait)
                resetEvent();

                // Always increment counter by 2 to keep it even
                counter++;
                counter++;

                // Signal the event to let waiter proceed
                setEvent();
            }
        }, "Signaler");

        waiter.start();
        signaler.start();
    }

    // Wait until the event is signaled
    private void waitForEvent() {
        syncLock.lock();
        try {
            while (!isSignaled) {
                try {
                    syncCondition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            syncLock.unlock();
        }
    }

    // Signal the event
    private void setEvent() {
        syncLock.lock();
        try {
            isSignaled = true;
            syncCondition.signalAll();
        } finally {
            syncLock.unlock();
        }
    }

    // Reset the event
    private void resetEvent() {
        syncLock.lock();
        try {
            isSignaled = false;
        } finally {
            syncLock.unlock();
        }
    }
}
