package org.pastalab.fray.test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pastalab.fray.core.TestRunner;
import org.pastalab.fray.core.command.Configuration;
import org.pastalab.fray.core.command.ExecutionInfo;
import org.pastalab.fray.core.command.LambdaExecutor;
import org.pastalab.fray.core.command.NetworkDelegateType;
import org.pastalab.fray.core.command.SystemTimeDelegateType;
import org.pastalab.fray.core.observers.TimelineCoverageType;
import org.pastalab.fray.core.randomness.ControlledRandomProvider;
import org.pastalab.fray.core.scheduler.POSScheduler;

/**
 * A {@link Condition} created in one iteration and awaited in a later one.
 *
 * <p>Every iteration starts with an empty map from conditions to their locks: {@code
 * RunContext.done()} clears it, and {@code TestRunner} builds a new {@code RunContext} per
 * iteration. Without the patch, a condition whose {@code newCondition()} ran in an earlier
 * iteration falls through to the map's default, which uses the condition itself as its lock. The
 * awaiting thread then does not hold that stand-in, so {@code objectWaitImpl} resumes it without
 * blocking and leaves it {@code Running}, while the JVM parks it in the real {@code await}. No
 * other thread is scheduled again, and the iteration never ends. With {@code fray.debug=true}, as
 * this suite runs, the guard on that default reports a {@code FrayInternalError} first.
 *
 * <p>Iterations of one test share the JVM's statics whenever the class loader is not reset per
 * iteration. That is the {@code @ConcurrencyTest} extension's setting, and the setting of any
 * harness that runs a lambda, so each test here runs a {@link LambdaExecutor} with {@code
 * resetClassLoader = false} and the {@code @ConcurrencyTest} defaults otherwise. A library keeps
 * such a condition when it holds a JVM-wide executor in a static field.
 *
 * <p>Every thread that creates, awaits or signals the condition is one the iteration starts under
 * Fray. The test thread only starts {@link TestRunner} and bounds it in wall-clock time, because an
 * unpatched run never returns.
 */
public class ConditionFromEarlierIterationTest {

  private static final int ITERATIONS = 20;

  /** Wall-clock bound on a whole run; a patched run takes well under a second. */
  private static final long RUN_BOUND_SECONDS = 60;

  @TempDir Path tempDir;

  private static ReentrantLock lock;
  private static Condition condition;
  private static boolean signalled;
  private static ScheduledThreadPoolExecutor executor;
  private static int iterationsDone;

  @BeforeEach
  void reset() {
    lock = null;
    condition = null;
    executor = null;
    iterationsDone = 0;
  }

  /** The first iteration creates the lock and condition; every later one reuses them. */
  private static void awaitAndSignal(boolean timed) throws InterruptedException {
    if (lock == null) {
      lock = new ReentrantLock();
      condition = lock.newCondition();
    }
    lock.lock();
    try {
      signalled = false;
    } finally {
      lock.unlock();
    }
    Thread waiter =
        new Thread(
            () -> {
              lock.lock();
              try {
                while (!signalled) {
                  if (timed) {
                    condition.awaitNanos(TimeUnit.SECONDS.toNanos(10));
                  } else {
                    condition.await();
                  }
                }
              } catch (InterruptedException e) {
                throw new IllegalStateException(e);
              } finally {
                lock.unlock();
              }
            });
    waiter.start();
    lock.lock();
    try {
      signalled = true;
      condition.signalAll();
    } finally {
      lock.unlock();
    }
    waiter.join();
    iterationsDone++;
  }

  @Test
  public void untimedAwaitOnConditionFromEarlierIteration() throws Exception {
    assertRunCompletes(() -> awaitAndSignal(false));
  }

  @Test
  public void timedAwaitOnConditionFromEarlierIteration() throws Exception {
    assertRunCompletes(() -> awaitAndSignal(true));
  }

  /**
   * A static {@link ScheduledThreadPoolExecutor}, created by the first iteration: its work queue's
   * condition outlives that iteration, and its worker in a later iteration waits on it.
   */
  @Test
  public void staticScheduledExecutorAcrossIterations() throws Exception {
    assertRunCompletes(
        () -> {
          if (executor == null) {
            executor = new ScheduledThreadPoolExecutor(1);
            executor.setKeepAliveTime(1, TimeUnit.SECONDS);
            executor.allowCoreThreadTimeOut(true);
          }
          try {
            executor.schedule(() -> {}, 10, TimeUnit.MILLISECONDS).get();
          } catch (ExecutionException e) {
            throw new IllegalStateException(e);
          }
          iterationsDone++;
        });
  }

  private interface Body {
    void run() throws InterruptedException;
  }

  private void assertRunCompletes(Body body) throws Exception {
    Configuration config =
        new Configuration(
            new ExecutionInfo(
                new LambdaExecutor(
                    () -> {
                      try {
                        body.run();
                      } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                      }
                      return null;
                    }),
                false,
                false,
                -1),
            tempDir,
            ITERATIONS,
            600,
            new POSScheduler(),
            new ControlledRandomProvider(),
            false, // fullSchedule
            false, // exploreMode
            true, // noExitWhenBugFound
            false, // isReplay
            false, // noFray
            false, // dummyRun
            NetworkDelegateType.NONE,
            SystemTimeDelegateType.NONE,
            100_000L,
            true, // ignoreTimedBlock
            false, // sleepAsYield
            false, // resetClassLoader, as @ConcurrencyTest: statics outlive an iteration
            false, // redirectStdout
            false, // abortThreadExecutionAfterMainExit
            false, // resolveRacingOperationStackTraceHash
            TimelineCoverageType.RESOURCE_ORDERING);
    AtomicReference<Throwable> result = new AtomicReference<>();
    // A daemon, so a run that never returns cannot keep the test JVM alive.
    Thread runner =
        new Thread(() -> result.set(new TestRunner(config).run()), "condition-test-runner");
    runner.setDaemon(true);
    runner.start();
    runner.join(TimeUnit.SECONDS.toMillis(RUN_BOUND_SECONDS));
    assertTrue(
        !runner.isAlive(),
        "the run did not finish within "
            + RUN_BOUND_SECONDS
            + " s; iterations completed: "
            + iterationsDone
            + " of "
            + ITERATIONS
            + " (a thread awaiting a condition from an earlier iteration left Fray's schedule)");
    assertNull(result.get(), "Fray reported an error");
    assertTrue(iterationsDone == ITERATIONS, "iterations completed: " + iterationsDone);
  }
}
