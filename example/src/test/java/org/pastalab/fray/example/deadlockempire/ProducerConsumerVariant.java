package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Translation of "Producer-Consumer (variant)" from The Deadlock Empire
 *
 * This demonstrates how non-thread safe collections can cause issues when
 * used concurrently without proper synchronization.
 *
 * Story: Only one factory line remains. If you disrupt this, the entire land
 * will be swept clean and the Empire will have lost all production.
 *
 * WIN CONDITION: Cause an exception to be raised due to non-thread safe queue operations.
 */
@ExtendWith(FrayTestExtension.class)
public class ProducerConsumerVariant extends DeadlockEmpireTestBase {
    private final Queue<String> queue = new LinkedList<>();

    @ConcurrencyTest
    public void runTest() {
        Thread producer = new Thread(() -> {
            while (true) {
                queue.add("Golem");
            }
        }, "The Producer");

        Thread consumer = new Thread(() -> {
            while (true) {
                if (!queue.isEmpty()) {
                    // This is unsafe - queue could become empty between the check and removal
                    queue.remove();
                }
            }
        }, "The Consumer");

        producer.start();
        consumer.start();
    }
}
