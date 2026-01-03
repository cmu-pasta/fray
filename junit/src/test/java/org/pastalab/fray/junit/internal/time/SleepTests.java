package org.pastalab.fray.junit.internal.time;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.core.command.SystemTimeDelegateType;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

@ExtendWith(FrayTestExtension.class)
public class SleepTests {

  @ConcurrencyTest(
      systemTimeDelegateType = SystemTimeDelegateType.MOCK,
      ignoreTimedBlock = false,
      sleepAsYield = false)
  public void testSleepInterrupt() throws InterruptedException {
    AtomicBoolean interrupted = new AtomicBoolean(false);
    Thread thread =
        new Thread(
            () -> {
              try {
                Thread.sleep(50);
              } catch (InterruptedException e) {
                interrupted.set(true);
              }
              assertFalse(
                  Thread.currentThread().isInterrupted(),
                  "Thread interrupt status should be cleared after InterruptedException");
            });
    thread.start();
    thread.interrupt();
    thread.join();

    assertTrue(interrupted.get(), "Thread should have been interrupted during sleep");
  }
}
