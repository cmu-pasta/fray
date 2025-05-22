package org.pastalab.fray.test;

import io.github.classgraph.ClassGraph;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.pastalab.fray.core.FrayInternalError;
import org.pastalab.fray.core.TestRunner;
import org.pastalab.fray.core.command.*;
import org.pastalab.fray.core.randomness.ControlledRandom;
import org.pastalab.fray.core.scheduler.PCTScheduler;
import org.pastalab.fray.core.scheduler.POSScheduler;
import org.pastalab.fray.test.controllers.network.reactive.success.NetworkCallWithSocketNoDeadlock;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class FrayTestCase {

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
                1000,
                60,
                new POSScheduler(),
                new ControlledRandom(),
                true,
                false,
                true,
                false,
                false,
                false,
                NetworkDelegateType.REACTIVE,
                TimeDelegateType.MOCK
        );
        TestRunner runner = new TestRunner(config);
        runner.run();
    }

    @TestFactory
    public List<DynamicTest> testCases() {
        List<DynamicTest> tests = new ArrayList<>();
        new ClassGraph().acceptPackages("org.pastalab.fray.test.core").scan().getSubclasses(Object.class.getName()).forEach((classInfo) -> {
            String name = classInfo.getName();
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
                    "/tmp/report",
                    1000,
                    60,
                    new PCTScheduler(),
                    new ControlledRandom(),
                    true,
                    false,
                    true,
                    false,
                    false,
                    false,
                    NetworkDelegateType.PROACTIVE,
                    TimeDelegateType.MOCK
            );
            tests.add(populateTest(classInfo.getName(), shouldFail, config));
        });
        return tests;
    }

    @TestFactory
    public List<DynamicTest> testReactiveNetworkController() {
        List<DynamicTest> tests = new ArrayList<>();
        new ClassGraph().acceptPackages("org.pastalab.fray.test.controllers.network.reactive").scan().getSubclasses(Object.class.getName()).forEach((classInfo) -> {
            String name = classInfo.getName();
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
                    "/tmp/report",
                    50,
                    60,
                    new PCTScheduler(),
                    new ControlledRandom(),
                    true,
                    false,
                    true,
                    false,
                    false,
                    false,
                    NetworkDelegateType.REACTIVE,
                    TimeDelegateType.MOCK
            );
            tests.add(populateTest(classInfo.getName(), shouldFail, config));
        });
        return tests;
    }
}
