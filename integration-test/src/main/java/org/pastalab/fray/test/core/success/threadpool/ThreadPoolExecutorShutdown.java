package org.pastalab.fray.test.core.success.threadpool;

import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolExecutorShutdown {
    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                0, 1024, 0L, java.util.concurrent.TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>());

        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                Thread.currentThread().interrupt();

                synchronized (Thread.currentThread()) {
                    try {
                        Thread.currentThread().wait();
                    } catch (InterruptedException e) {
                    }
                }
            });
        }
        new Thread(() -> {
            executor.shutdown();
        }).start();
    }
}
