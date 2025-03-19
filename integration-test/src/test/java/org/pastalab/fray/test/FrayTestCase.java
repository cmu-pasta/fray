package org.anonlab.fray.test;

import io.github.classgraph.ClassGraph;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.anonlab.fray.core.FrayInternalError;
import org.anonlab.fray.core.TestRunner;
import org.anonlab.fray.core.command.Configuration;
import org.anonlab.fray.core.command.ExecutionInfo;
import org.anonlab.fray.core.command.LambdaExecutor;
import org.anonlab.fray.core.command.MethodExecutor;
import org.anonlab.fray.core.randomness.ControlledRandom;
import org.anonlab.fray.core.scheduler.PCTScheduler;
import org.anonlab.fray.core.scheduler.POSScheduler;
import org.anonlab.fray.test.fail.thread.ThreadExitDeadlock;
import org.anonlab.fray.test.success.thread.ThreadInterruptWithReentrantLockUnlock;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class FrayTestCase {

    private DynamicTest populateTest(String className, boolean testShouldFail) {
        Configuration config = new Configuration(
                new ExecutionInfo(
                        new MethodExecutor(className,
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
                false
        );
        TestRunner runner = new TestRunner(config);
        return dynamicTest("Test: " + className, () -> {
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
                                    ThreadInterruptWithReentrantLockUnlock.main(new String[]{});
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
                    false
            );
            TestRunner runner = new TestRunner(config);
            runner.run();
    }

    @TestFactory
    public List<DynamicTest> testCases() {
        List<DynamicTest> tests = new ArrayList<>();
        new ClassGraph().acceptPackages("org.anonlab.fray.test").scan().getSubclasses(Object.class.getName()).forEach((classInfo) -> {
            String name = classInfo.getName();
            boolean shouldFail = true;
            if (name.contains("fail")) {
                shouldFail = true;
            } else if (name.contains("success")) {
                shouldFail = false;
            } else {
                return;
            }
            tests.add(populateTest(classInfo.getName(), shouldFail));
        });
        return tests;
    }
}
