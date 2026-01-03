package org.pastalab.fray.junit.internal.syncurity;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.pastalab.fray.junit.ranger.ConditionFactoryKt.await;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

@ExtendWith(FrayTestExtension.class)
public class SyncurityAwaitTest {
  @ConcurrencyTest(iterations = 100)
  public void testConstraintSatisfied() {
    AtomicInteger i = new AtomicInteger(0);
    Thread t =
        new Thread(
            () -> {
              i.incrementAndGet();
            });
    t.start();
    await().untilAtomic(i, equalTo(1));
    assertEquals(1, i.get());
  }

  @ConcurrencyTest(iterations = 100)
  public void testConstraintDeadlock() {
    AtomicInteger i = new AtomicInteger(0);
    await().untilAtomic(i, equalTo(1));
    assertEquals(1, i.get());
  }

  @ConcurrencyTest(iterations = 100)
  public void testInThreadConstraintDeadlock() throws InterruptedException {
    AtomicInteger i = new AtomicInteger(0);
    Thread t =
        new Thread(
            () -> {
              await().untilAtomic(i, equalTo(1));
              assertEquals(1, i.get());
            });
    t.start();
    t.join();
  }
}
