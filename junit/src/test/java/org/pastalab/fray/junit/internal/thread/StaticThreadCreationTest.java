package org.pastalab.fray.junit.internal.thread;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.FrayTest;

@ExtendWith(FrayTestExtension.class)
public class StaticThreadCreationTest {
  public static Thread t =
      new Thread(
          () -> {
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          });

  @FrayTest
  public void testStaticThreadCreation() throws InterruptedException {
    t.start();
    t.join();
  }
}
