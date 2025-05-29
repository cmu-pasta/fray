package org.pastalab.fray.test.core.success.threadpool;

import javax.swing.plaf.basic.BasicTreeUI;
import java.util.concurrent.*;

public class ScheduledThreadPoolWorkSteal {
    public static void main(String[] args) throws InterruptedException {
        // Create a ScheduledThreadPoolExecutor with 1 core thread
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

        // Clean shutdown
        new Thread(() -> {
            executor.shutdown();
            System.out.println("Shutting down executor...");
        }).start();
        System.out.println("Starting test to trigger DelayedWorkQueue.take() leader code...");

        // Schedule a task with a 2-second delay
        // This will cause the task to be added to the DelayedWorkQueue
        try {
            ScheduledFuture<?> future = executor.schedule(() -> {
                System.out.println("Delayed task executed at: " + System.currentTimeMillis());
                Thread.yield();
            }, 2, TimeUnit.SECONDS);
            // Wait for the task to complete
            try {
                future.get(); // This will block until the scheduled task completes
                System.out.println("Task completed successfully");
                Thread.yield();
            } catch (Throwable e) {
                System.err.println("Task execution failed: " + e.getCause());
            }
        } catch (RejectedExecutionException e) {}

        System.out.println("Task scheduled with 2-second delay at: " + System.currentTimeMillis());

    }
}