package org.pastalab.fray.example.deadlockempire;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Semaphore;

/**
 * Base class for Deadlock Empire examples
 */
public abstract class DeadlockEmpireTestBase {

    // Keep track of threads in critical section
    protected AtomicInteger threadsInCriticalSection = new AtomicInteger(0);

    /**
     * Enter critical section and check for race condition
     */
    protected void criticalSection() {
        int count = threadsInCriticalSection.incrementAndGet();
        if (count > 1) {
            System.out.println("Race condition detected: " + count + " threads in critical section!");
            throw new RuntimeException();
        }
        threadsInCriticalSection.decrementAndGet();
    }

    /**
     * Signal that a failure has occurred
     */
    protected void failure() {
        System.out.println("Failure condition reached by " + Thread.currentThread().getName());
        throw new RuntimeException();
    }
}
