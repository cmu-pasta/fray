package org.pastalab.fray.junit.internal.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.core.command.SystemTimeDelegateType;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

@ExtendWith(FrayTestExtension.class)
public class DeterministicVirtualTimeTest {

  @ConcurrencyTest(
      iterations = 1000,
      ignoreTimedBlock = false,
      sleepAsYield = false,
      systemTimeDelegateType = SystemTimeDelegateType.MOCK)
  public void testTimeDeterministicIncrement() throws InterruptedException {
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < 1000; i++) {
      System.currentTimeMillis();
    }
    assertEquals(100, System.currentTimeMillis() - startTime);

    startTime = System.currentTimeMillis();
    Thread.sleep(10);
    assertTrue(System.currentTimeMillis() - startTime >= 10);

    startTime = System.currentTimeMillis();
    CountDownLatch latch = new CountDownLatch(1);
    Thread t =
        new Thread(
            () -> {
              try {
                assertFalse(latch.await(10, java.util.concurrent.TimeUnit.MILLISECONDS));
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            });

    t.start();
    t.join();
    assertTrue(System.currentTimeMillis() - startTime >= 10);
  }
}
