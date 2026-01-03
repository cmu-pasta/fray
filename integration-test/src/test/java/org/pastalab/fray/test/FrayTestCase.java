package org.pastalab.fray.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import io.github.classgraph.ClassGraph;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import org.pastalab.fray.core.FrayInternalError;
import org.pastalab.fray.core.TestRunner;
import org.pastalab.fray.core.command.Configuration;
import org.pastalab.fray.core.command.ExecutionInfo;
import org.pastalab.fray.core.command.LambdaExecutor;
import org.pastalab.fray.core.command.MethodExecutor;
import org.pastalab.fray.core.command.NetworkDelegateType;
import org.pastalab.fray.core.command.SystemTimeDelegateType;
import org.pastalab.fray.core.randomness.ControlledRandom;
import org.pastalab.fray.core.randomness.ControlledRandomProvider;
import org.pastalab.fray.core.randomness.RecordedRandomProvider;
import org.pastalab.fray.core.scheduler.RandomScheduler;
import org.pastalab.fray.core.scheduler.Scheduler;
import org.pastalab.fray.test.core.fail.monitor.MonitorDeadlock;
import org.pastalab.fray.test.core.success.threadpool.ScheduledThreadPoolWorkSteal;

public class FrayTestCase {

  @TempDir Path tempDir;

  private DynamicTest populateTest(
      String className, boolean testShouldFail, Configuration configuration) {
    return dynamicTest(
        "Test: " + className,
        () -> {
          TestRunner runner = new TestRunner(configuration);
          Throwable result = runner.run();
          if (testShouldFail) {
            assertFalse(result instanceof FrayInternalError);
            assertNotEquals(null, result);
          } else {
            assertEquals(null, result);
          }
        });
  }

  public void replay() throws IOException {
    String basePath = "/tmp/report-ba/recording/";
    Scheduler scheduler = org.pastalab.fray.core.utils.UtilsKt.schedulerFromRecording(basePath);
    RecordedRandomProvider randomnessProvider =
        new RecordedRandomProvider(basePath + "/random.json");
    Configuration config =
        new Configuration(
            new ExecutionInfo(
                new LambdaExecutor(
                    () -> {
                      try {
                        ScheduledThreadPoolWorkSteal.main(new String[] {});
                      } catch (Exception e) {
                        throw new RuntimeException(e);
                      }
                      return null;
                    }),
                false,
                false,
                -1),
            "/tmp/report2",
            1,
            60,
            scheduler,
            randomnessProvider,
            true,
            false,
            true,
            true,
            false,
            false,
            NetworkDelegateType.REACTIVE,
            SystemTimeDelegateType.MOCK,
            true,
            false);
    TestRunner runner = new TestRunner(config);
    runner.run();
  }

  @Test
  public void testOne() throws Throwable {
    System.setProperty("fray.recordSchedule", "true");
    Configuration config =
        new Configuration(
            new ExecutionInfo(
                new LambdaExecutor(
                    () -> {
                      try {
                        MonitorDeadlock.main(new String[] {});
                      } catch (Exception e) {
                        throw new RuntimeException(e);
                      }
                      return null;
                    }),
                false,
                false,
                -1),
            "/tmp/report2",
            2,
            60,
            new RandomScheduler(
                new ControlledRandom(new ArrayList<>(), new ArrayList<>(), new Random(0))),
            new ControlledRandomProvider(),
            true,
            false,
            true,
            false,
            false,
            false,
            NetworkDelegateType.REACTIVE,
            SystemTimeDelegateType.NONE,
            true,
            false);
    TestRunner runner = new TestRunner(config);
    runner.run();
  }

  private List<DynamicTest> populateTests(
      String dirName,
      int iteration,
      NetworkDelegateType networkDelegateType,
      SystemTimeDelegateType systemTimeDelegateType,
      boolean ignoredTimedBlock) {
    List<DynamicTest> tests = new ArrayList<>();
    new ClassGraph()
        .acceptPackages(dirName)
        .scan()
        .getSubclasses(Object.class.getName())
        .forEach(
            (classInfo) -> {
              String name = classInfo.getName();
              if (name.contains("ScheduledThreadPoolWorkSteal")) {
                return;
              }
              // Do not run subclasses
              if (name.contains("$")) {
                return;
              }
              boolean shouldFail = true;
              if (name.contains("fail")) {
                shouldFail = true;
              } else if (name.contains("success")) {
                shouldFail = false;
              } else {
                return;
              }
              Configuration config =
                  new Configuration(
                      new ExecutionInfo(
                          new MethodExecutor(
                              classInfo.getName(),
                              "main",
                              new ArrayList<>(),
                              new ArrayList<>(),
                              new HashMap<>()),
                          false,
                          false,
                          -1),
                      tempDir.toString(),
                      iteration,
                      60 * 10,
                      new RandomScheduler(),
                      new ControlledRandomProvider(),
                      true,
                      false,
                      true,
                      false,
                      false,
                      false,
                      networkDelegateType,
                      systemTimeDelegateType,
                      ignoredTimedBlock,
                      false);
              tests.add(populateTest(classInfo.getName(), shouldFail, config));
            });
    return tests;
  }

  @TestFactory
  public List<DynamicTest> testCases() {
    return populateTests(
        "org.pastalab.fray.test.core",
        100,
        NetworkDelegateType.PROACTIVE,
        SystemTimeDelegateType.MOCK,
        true);
  }

  @TestFactory
  public List<DynamicTest> testReactiveNetworkController() {
    return populateTests(
        "org.pastalab.fray.test.controllers.network.reactive.success.inputstream",
        10,
        NetworkDelegateType.REACTIVE,
        SystemTimeDelegateType.NONE,
        true);
  }

  @TestFactory
  public List<DynamicTest> testTimedOperations() {
    return populateTests(
        "org.pastalab.fray.test.time",
        10,
        NetworkDelegateType.REACTIVE,
        SystemTimeDelegateType.MOCK,
        false);
  }
}
