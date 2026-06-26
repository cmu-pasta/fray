package org.pastalab.fray.junit.internal.fest;

import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.core.observers.TimelineCoverageType;
import org.pastalab.fray.core.scheduler.FestScheduler;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.FrayTest;

@ExtendWith(FrayTestExtension.class)
public class FestSchedulerTest {

  static int sharedCounter;

  private static void contend() throws InterruptedException {
    sharedCounter = 0;
    ReentrantLock lock = new ReentrantLock();
    Runnable worker =
        () -> {
          for (int i = 0; i < 3; i++) {
            lock.lock();
            try {
              sharedCounter++;
            } finally {
              lock.unlock();
            }
          }
        };
    Thread t1 = new Thread(worker, "worker-1");
    Thread t2 = new Thread(worker, "worker-2");
    Thread t3 = new Thread(worker, "worker-3");
    t1.start();
    t2.start();
    t3.start();
    t1.join();
    t2.join();
    t3.join();
    // The counter is fully synchronized, so this invariant holds under every interleaving Fest
    // explores. If Fest produced an infeasible or broken schedule the run would crash instead.
    assert sharedCounter == 9;
  }

  @FrayTest(
      scheduler = FestScheduler.class,
      iterations = 200,
      collectTimelineCoverage = TimelineCoverageType.RESOURCE_ORDERING)
  public void festWithResourceOrderingCoverage() throws InterruptedException {
    contend();
  }

  @FrayTest(
      scheduler = FestScheduler.class,
      iterations = 200,
      collectTimelineCoverage = TimelineCoverageType.THREAD_ORDERING)
  public void festWithThreadOrderingCoverage() throws InterruptedException {
    contend();
  }

  // Fest needs a feedback signal; when the user disables coverage it falls back to a default one,
  // so this still runs as a real feedback-guided search rather than blind mutation.
  @FrayTest(
      scheduler = FestScheduler.class,
      iterations = 200,
      collectTimelineCoverage = TimelineCoverageType.NONE)
  public void festWithNoCoverageFallsBackToDefault() throws InterruptedException {
    contend();
  }
}
