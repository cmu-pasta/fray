package org.pastalab.fray.junit.internal.time;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.core.command.SystemTimeDelegateType;
import org.pastalab.fray.core.scheduler.FifoScheduler;
import org.pastalab.fray.core.scheduler.RandomScheduler;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.FrayTest;

@ExtendWith(FrayTestExtension.class)
public class TimedWaitSmallIntervalTests {

  @FrayTest(
      ignoreTimedBlock = false,
      scheduler = RandomScheduler.class,
      systemTimeDelegateType = SystemTimeDelegateType.MOCK)
  public void testTimedWaitSmallIntervalNormal() throws InterruptedException {
    Object o = new Object();
    for (int i = 0; i < 10; i++) {
      Thread t =
          new Thread(
              () -> {
                for (int j = 0; j < 10; j++) {
                  try {
                    synchronized (o) {
                      o.wait(100);
                    }
                  } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                  }
                }
              });
      t.start();
    }
    AtomicInteger count = new AtomicInteger();
    Thread t =
        new Thread(
            () -> {
              for (int i = 0; i < 100; i++) {
                count.incrementAndGet();
              }
            });
    t.start();
  }

  @FrayTest(
      ignoreTimedBlock = false,
      scheduler = FifoScheduler.class,
      systemTimeDelegateType = SystemTimeDelegateType.MOCK)
  public void testTimedWaitSmallIntervalIllegalMonitorException() throws InterruptedException {
    Object o = new Object();
    for (int i = 0; i < 10; i++) {
      Thread t =
          new Thread(
              () -> {
                for (int j = 0; j < 10; j++) {
                  try {
                    o.wait(100);
                  } catch (Exception e) {
                  }
                }
              });
      t.start();
    }
    AtomicInteger count = new AtomicInteger();
    Thread t =
        new Thread(
            () -> {
              for (int i = 0; i < 100; i++) {
                count.incrementAndGet();
              }
            });
    t.start();
  }
}
