package org.pastalab.fray.test;

import io.github.classgraph.ClassGraph;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import org.pastalab.fray.core.FrayInternalError;
import org.pastalab.fray.core.TestRunner;
import org.pastalab.fray.core.command.*;
import org.pastalab.fray.core.randomness.ControlledRandom;
import org.pastalab.fray.core.scheduler.PCTScheduler;
import org.pastalab.fray.core.scheduler.RandomScheduler;
import org.pastalab.fray.core.scheduler.Scheduler;
import org.pastalab.fray.core.utils.UtilsKt;
import org.pastalab.fray.test.controllers.network.reactive.success.NetworkCallWithSocketNoDeadlock;
import org.pastalab.fray.test.core.fail.wait.TwoWaitDeadlock;
import org.pastalab.fray.test.core.success.abq.ArrayBlockingQueueNormalOffer;
import org.pastalab.fray.test.core.success.condition.ConditionAwaitStaticField;
import org.pastalab.fray.test.core.success.network.AsyncServerAsyncClient;
import org.pastalab.fray.test.core.success.threadpool.ScheduledThreadPoolWorkSteal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class FrayTestCase {

    @TempDir
    Path tempDir;

    private DynamicTest populateTest(String className, boolean testShouldFail, Configuration configuration) {
        return dynamicTest("Test: " + className, () -> {
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
        ControlledRandom randomnessProvider = UtilsKt.randomFromRecording(basePath);
        Configuration config = new Configuration(
                new ExecutionInfo(
                        new LambdaExecutor(() -> {
                            try {
                                ScheduledThreadPoolWorkSteal.main(new String[]{});
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            return null;
                        }),
                        false,
                        false,
                        -1
                ),
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
                false
        );
        TestRunner runner = new TestRunner(config);
        runner.run();
    }

    @Test
    public void testOne() throws Throwable {
        System.setProperty("fray.recordSchedule", "true");
        Configuration config = new Configuration(
                new ExecutionInfo(
                        new LambdaExecutor(() -> {
                            try {
                                NetworkCallWithSocketNoDeadlock.main(new String[]{});
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            return null;
                        }),
                        false,
                        false,
                        -1
                ),
                "/tmp/report2",
                50,
                60,
                new RandomScheduler(new ControlledRandom()),
                new ControlledRandom(),
                true,
                true,
                true,
                false,
                false,
                false,
                NetworkDelegateType.REACTIVE,
                SystemTimeDelegateType.NONE,
                true,
                false
        );
        TestRunner runner = new TestRunner(config);
        runner.run();
    }

    private List<DynamicTest> populateTests(String dirName, int iteration, NetworkDelegateType networkDelegateType,
                                            SystemTimeDelegateType systemTimeDelegateType, boolean ignoredTimedBlock) {
        List<DynamicTest> tests = new ArrayList<>();
        new ClassGraph().acceptPackages(dirName).scan().getSubclasses(Object.class.getName()).forEach((classInfo) -> {
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
            Configuration config = new Configuration(
                    new ExecutionInfo(
                            new MethodExecutor(classInfo.getName(),
                                    "main",
                                    new ArrayList<>(),
                                    new ArrayList<>(),
                                    new HashMap<>()
                            ),
                            false,
                            false,
                            -1
                    ),
                    tempDir.toString(),
                    iteration,
                    60 * 10,
                    new PCTScheduler(),
                    new ControlledRandom(),
                    true,
                    false,
                    true,
                    false,
                    false,
                    false,
                    networkDelegateType,
                    systemTimeDelegateType,
                    ignoredTimedBlock,
                    false
            );
            tests.add(populateTest(classInfo.getName(), shouldFail, config));
        });
        return tests;
    }

    @TestFactory
    public List<DynamicTest> testCases() {
        return populateTests("org.pastalab.fray.test.core", 100, NetworkDelegateType.PROACTIVE,
                SystemTimeDelegateType.MOCK, true);
    }

    @TestFactory
    public List<DynamicTest> testReactiveNetworkController() {
        return populateTests("org.pastalab.fray.test.controllers.network.reactive", 10, NetworkDelegateType.REACTIVE,
                SystemTimeDelegateType.MOCK, true);
    }

    @TestFactory
    public List<DynamicTest> testTimedOperations() {
        return populateTests("org.pastalab.fray.test.time", 10, NetworkDelegateType.REACTIVE,
                SystemTimeDelegateType.MOCK, false);
    }
}
