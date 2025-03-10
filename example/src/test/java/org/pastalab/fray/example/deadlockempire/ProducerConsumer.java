package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

/**
 * Translation of "Producer-Consumer" from The Deadlock Empire
 *
 * This demonstrates how semaphores can be used incorrectly in producer-consumer scenarios.
 *
 * Story: You pick your target - one dragon-producing factory is sending its creations
 * directly into an armory that outfits the machines with destructive weapons.
 * If you disrupt this supply line, you will greatly reduce the number of dragons
 * at the Empire's disposal.
 *
 * WIN CONDITION: Cause an exception to be raised.
 */
@ExtendWith(FrayTestExtension.class)
public class ProducerConsumer extends DeadlockEmpireTestBase {
    private final Queue<String> queue = new LinkedList<>();
    private final Semaphore semaphore = new Semaphore(0);

    @ConcurrencyTest
    public void runTest() {
        Thread consumer = new Thread(() -> {
            while (true) {
                if (semaphore.tryAcquire()) {
                    // This is unsafe - queue could be empty even if semaphore has permits
                    String dragon = queue.remove(); // May throw NoSuchElementException
                } else {
                    // Nothing in the queue
                }
            }
        }, "Consumer");

        Thread producer = new Thread(() -> {
            while (true) {
                semaphore.release();
                queue.add("Dragon");
            }
        }, "Producer");

        consumer.start();
        producer.start();
    }
}
