package org.pastalab.fray.junit.internal.thread;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.FrayTest;

@ExtendWith(FrayTestExtension.class)
public class TestThreadTerminationAfterMainExit {
  public volatile boolean flag = false;

  @FrayTest(abortThreadExecutionAfterMainExit = true)
  public void testThreadTerminationAfterMainExit() {
    Thread thread =
        new Thread(
            () -> {
              while (!flag) {}
            });
    thread.start();
  }
}
