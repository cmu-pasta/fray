package org.pastalab.fray.test.core.success.threadpool;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ScheduledThreadPoolWorkSteal {
  public static void main(String[] args) throws InterruptedException {
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    new Thread(
            () -> {
              executor.shutdown();
            })
        .start();
    try {
      ScheduledFuture<?> future =
          executor.schedule(
              () -> {
                Thread.yield();
              },
              10,
              TimeUnit.MILLISECONDS);
      try {
        future.get(); // This will block until the scheduled task completes
        Thread.yield();
      } catch (Throwable e) {
      }
    } catch (RejectedExecutionException e) {
    }
  }
}
