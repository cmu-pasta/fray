package org.pastalab.fray.test.time.success;

public class ThreadSleepTimeCheck {
    public static void main(String[] args) throws InterruptedException {
        long currentTime = System.nanoTime();
        Thread.sleep(1000);
        long elapsedTime = System.nanoTime() - currentTime;
        assert(elapsedTime >= 1000000000L); // Ensure at least 1 second has passed
    }
}
