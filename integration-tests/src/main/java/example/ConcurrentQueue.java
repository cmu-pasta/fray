package example;

import java.util.LinkedList;

import cmu.pasta.sfuzz.core.GlobalContext;

/**
 * This is a thread-safe fixed-size queue data structure that blocks on:
 *   - inserts, when it is full (until an element is consumed),
 *   - removals, when it is empty (until an element is produced).
 */
class ConcurrentQueue<T> {
    private LinkedList<T> queue;
    private Integer maxSize;

    private void log (String format, Object... args) {
        GlobalContext.INSTANCE.log(format, args);
    }

    public ConcurrentQueue(int size) {
        queue = new LinkedList<T>();
        maxSize = size;
    }

    // removes and returns the last element from the queue
    public T consume() throws InterruptedException {
        log("consume() called. Trying to acquire lock.");
        synchronized(this) {
            log("Got lock.");
            while(queue.size() == 0) {
                log("Waiting until queue size != 0.");
                this.wait();
            }
            T toReturn = queue.peek();
            queue.remove();
            log("Notifying all.");
            this.notifyAll();
            log("consume() done.");
            return toReturn;
        }
        
    }

    // inserts an element into the queue
    public void produce(T element) throws InterruptedException {
        log("produce(%s) called. Trying to acquire lock.", element);
        synchronized(this) {
            log("Got lock.");
            while (queue.size() == maxSize) {
                log("Waiting till queue not full.");
                this.wait();
            }
        }

        log("Trying to acquire second lock.");
        synchronized(this) {
            log("Got lock.");
            queue.add(element);
            if (queue.size() > 0) {
                log("Notifying all since queue is non-empty.");
                this.notifyAll();
            }
        }
        log("produce(%s) done.", element);
    }

    // helper function to enable writing asserts for tests
    public LinkedList<T> getAllValues() {
        synchronized(this) {
            return queue;
        }
    }
}