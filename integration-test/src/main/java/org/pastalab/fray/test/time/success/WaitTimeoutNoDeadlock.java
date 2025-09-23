package org.pastalab.fray.test.time.success;

public class WaitTimeoutNoDeadlock {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        Object lock = new Object();
        try {
            synchronized (lock) {
                lock.wait(1000); // Wait for 1 second
            }
        } catch (InterruptedException e) {
            // This is expected
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        assert(elapsedTime >= 1000); // Ensure at least 1 second has passed
    }
}
