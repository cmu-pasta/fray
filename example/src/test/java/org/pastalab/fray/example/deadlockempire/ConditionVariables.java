package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Translation of "Condition Variables" from The Deadlock Empire
 *
 * This demonstrates the use of condition variables (wait/notify) for thread
 * synchronization, and the potential issues that can arise with them.
 *
 * Story: Condition variables are, unfortunately, still a rather difficult topic.
 * They're just hard. Try. If you fail, skip.
 *
 * WIN CONDITION: Find a way to cause a race condition or exception.
 */
@ExtendWith(FrayTestExtension.class)
public class ConditionVariables extends DeadlockEmpireTestBase {
    private final Object mutex = new Object();
    private final Queue<Integer> queue = new LinkedList<>();

    @ConcurrencyTest
    public void runTest() {
        Thread consumer1 = new Thread(() -> {
            while (true) {
                synchronized (mutex) {
                    // If queue is empty, wait until producer adds something
                    if (queue.isEmpty()) {
                        try {
                            // Release lock and wait to be notified
                            mutex.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    // Dequeue an item from the queue - might throw exception if queue is empty
                    Integer item = queue.remove();
                }
            }
        }, "Consumer 1");

        Thread consumer2 = new Thread(() -> {
            while (true) {
                synchronized (mutex) {
                    // If queue is empty, wait until producer adds something
                    if (queue.isEmpty()) {
                        try {
                            // Release lock and wait to be notified
                            mutex.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    // Dequeue an item from the queue - might throw exception if queue is empty
                    Integer item = queue.remove();
                }
            }
        }, "Consumer 2");

        Thread producer = new Thread(() -> {
            while (true) {
                synchronized (mutex) {
                    // Add an item to the queue
                    queue.add(42);

                    // Wake up all waiting threads
                    mutex.notifyAll();
                }
            }
        }, "Producer");

        consumer1.start();
        consumer2.start();
        producer.start();
    }
}
