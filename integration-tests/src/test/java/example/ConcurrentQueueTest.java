package example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class ConcurrentQueueTest {
    protected static final int QUEUE_SIZE = 8;
    protected static final int MAX_INPUT_SIZE_1 = QUEUE_SIZE;
    protected static final int MAX_INPUT_SIZE_2 = 2 * QUEUE_SIZE;

    // public void testConcurrentQueue(@Size(min=SIZE, max=SIZE) List<Integer> input, @From(RandomScheduleGenerator.class) Schedule s) throws InterruptedException {
    public void testConcurrentQueue(int numConsumed) throws InterruptedException {
        ArrayList<Integer> input = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
        final Integer numConsumedT = numConsumed;

        ConcurrentQueue<Integer> q = new ConcurrentQueue<>(QUEUE_SIZE);
        LinkedList<Integer> values = new LinkedList<Integer>();

        Thread producer = new Thread (() -> {
            for (Integer element : input) {
                try {
                    q.produce(element);
                } catch (InterruptedException e) {
                    System.out.println("[Producer]: InterruptedException caught: " + e);
                    return;
                }
            }
        });

        Thread consumer = new Thread (() -> {
            try {
                for (int i = 0; i < numConsumedT; i++) {
                    values.add(q.consume());
                }
            } catch (InterruptedException e) {
                System.out.println("[Consumer]: InterruptedException caught: " + e);
                return;
            }
        });

        producer.start();
        consumer.start();

        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            System.out.println("[main]: InterruptedException caught during join: " + e);
            throw e;
        }

        assertEquals(input.subList(0, numConsumedT), values);
        for (int i = 0; i < MAX_INPUT_SIZE_1 - numConsumedT; i++) {
            assertEquals(input.get(i + numConsumedT), q.getAllValues().get(i));
        }
    }

    // public void testConcurrentQueue(@Size(min=MAX_INPUT_SIZE_2, max=MAX_INPUT_SIZE_2) List<Integer> input, @From(RandomScheduleGenerator.class) Schedule s) throws InterruptedException {
    public void testConcurrentQueue2(int numConsumed) throws InterruptedException {
        ArrayList<Integer> input = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16));
        final Integer numConsumedT = numConsumed;

        ConcurrentQueue<Integer> q = new ConcurrentQueue<>(QUEUE_SIZE);
        Queue<Integer> values1 = new LinkedList<Integer>();
        Queue<Integer> values2 = new LinkedList<Integer>();

        Thread producer = new Thread (() -> {
            for (Integer element : input) {
                try {
                    q.produce(element);
                } catch (InterruptedException e) {
                    System.out.println("[Producer]: InterruptedException caught: " + e);
                    return;
                }
            }
        });

        Thread consumer1 = new Thread (() -> {
            try {
                for (int i = 0; i < numConsumedT / 2; i++) {
                    values1.add(q.consume());
                }
            } catch (InterruptedException e) {
                System.out.println("[Consumer1]: InterruptedException caught: " + e);
                return;
            }
        });

        Thread consumer2 = new Thread (() -> {
            try {
                for (int i = numConsumedT / 2; i < numConsumedT; i++) {
                    values2.add(q.consume());
                }
            } catch (InterruptedException e) {
                System.out.println("[Consumer2]: InterruptedException caught: " + e);
                return;
            }
        });

        producer.start();
        consumer1.start();
        consumer2.start();

        try {
            producer.join();
            consumer1.join();
            consumer2.join();
        } catch (InterruptedException e) {
            System.out.println("[main]: InterruptedException caught during join: " + e);
            throw e;
        }

        assertEquals(q.getAllValues().size(), MAX_INPUT_SIZE_2 - numConsumedT);
        assertEquals(values1.size() + values2.size(), (int) numConsumedT);
    }

    // helper function with the test logic called inside the try-with-schedule blocks during differential fuzzing
    private void doProduceConsume(ArrayList<Integer> input, ConcurrentQueue<Integer> q,
                                  Queue<Integer> values1, Queue<Integer> values2,
                                  Integer numConsumed, Integer numConsumedT) throws InterruptedException {
        Thread producer = new Thread (() -> {
            for (Integer element : input) {
                try {
                    q.produce(element);
                } catch (InterruptedException e) {
                    System.out.println("[Producer]: InterruptedException caught: " + e);
                    return;
                }
            }
        });

        Thread consumer1 = new Thread (() -> {
            try {
                for (int i = 0; i < numConsumedT / 2; i++) {
                    values1.add(q.consume());
                }
            } catch (InterruptedException e) {
                System.out.println("[Consumer1]: InterruptedException caught: " + e);
                return;
            }
        });

        Thread consumer2 = new Thread (() -> {
            try {
                for (int i = numConsumedT / 2; i < numConsumedT; i++) {
                    values2.add(q.consume());
                }
            } catch (InterruptedException e) {
                System.out.println("[Consumer2]: InterruptedException caught: " + e);
                return;
            }
        });

        producer.start();
        consumer1.start();
        consumer2.start();

        try {
            producer.join();
            consumer1.join();
            consumer2.join();
        } catch (InterruptedException e) {
            System.out.println("[main]: InterruptedException caught during join: " + e);
            throw e;
        }

        assertEquals(q.getAllValues().size(), MAX_INPUT_SIZE_2 - numConsumedT);
        assertEquals(values1.size() + values2.size(), (int)numConsumed);
    }

}
