package org.pastalab.fray.example.deadlockempire;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple queue implementation using an array and atomic integers.
 * This queue is bounded by the capacity provided at creation time.
 * It is not fully thread-safe but demonstrates basic concurrency concepts.
 */
public class SimpleAtomicQueue<E> {
    private final Object[] elements;
    private final int capacity;
    private final AtomicInteger head = new AtomicInteger(0);
    private final AtomicInteger tail = new AtomicInteger(0);
    private final AtomicInteger size = new AtomicInteger(0);

    /**
     * Creates a new queue with the specified capacity.
     *
     * @param capacity the maximum number of elements the queue can hold
     */
    public SimpleAtomicQueue(int capacity) {
        this.capacity = capacity;
        this.elements = new Object[capacity];
    }

    /**
     * Adds an element to the tail of the queue.
     * If the queue is full, the behavior is undefined.
     *
     * @param element the element to add
     * @return true if the element was added successfully
     */
    public boolean add(E element) {
        if (size.get() >= capacity) {
            return false; // Queue is full
        }
        size.incrementAndGet();
        int current = tail.getAndUpdate(t -> (t + 1) % capacity);
        elements[current] = element;
        return true;
    }

    /**
     * Removes and returns the element at the head of the queue.
     *
     * @return the element at the head of the queue
     * @throws NoSuchElementException if the queue is empty
     */
    @SuppressWarnings("unchecked")
    public E remove() {
        if (size.get() <= 0) {
            throw new NoSuchElementException("Queue is empty");
        }

        int current = head.getAndUpdate(h -> (h + 1) % capacity);
        E element = (E) elements[current];
        assert(element != null);
        elements[current] = null; // Help GC
        size.decrementAndGet();
        return element;
    }

    /**
     * Returns true if the queue is empty.
     *
     * @return true if the queue contains no elements
     */
    public boolean isEmpty() {
        return size.get() == 0;
    }

    /**
     * Returns the number of elements in the queue.
     *
     * @return the number of elements in the queue
     */
    public int size() {
        return size.get();
    }
}