package org.pastalab.fray.test;

import io.github.classgraph.ClassGraph;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.pastalab.fray.core.TestRunner;
import org.pastalab.fray.core.command.Configuration;
import org.pastalab.fray.core.command.ExecutionInfo;
import org.pastalab.fray.core.command.LambdaExecutor;
import org.pastalab.fray.core.command.MethodExecutor;
import org.pastalab.fray.core.randomness.ControlledRandom;
import org.pastalab.fray.core.scheduler.PCTScheduler;
import org.pastalab.fray.core.scheduler.POSScheduler;
import org.pastalab.fray.core.scheduler.RandomScheduler;
import org.pastalab.fray.test.fail.cdl.CountDownLatchDeadlockUnblockMultiThread;
import org.pastalab.fray.test.fail.park.ParkDeadlock;
import org.pastalab.fray.test.fail.rwlock.ReentrantReadWriteLockDeadlock;
import org.pastalab.fray.test.fail.wait.NotifyOrder;
import org.pastalab.fray.test.success.condition.ConditionAwaitTimeoutNotifyInterrupt;
import org.pastalab.fray.test.success.rwlock.ReentrantReadWriteLockDowngradingNoDeadlock;
import org.pastalab.fray.test.success.rwlock.ReentrantReadWriteLockNoDeadlock;
import org.pastalab.fray.test.success.stampedlock.StampedLockTryLockNoDeadlock;
import org.pastalab.fray.test.success.thread.ThreadInterruptionWithoutStart;
import org.pastalab.fray.test.success.time.TestTime;

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
                10000,
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
                                    CountDownLatchDeadlockUnblockMultiThread.main(new String[]{});
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
                    200,
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
            runner.run();
    }

    @TestFactory
    public List<DynamicTest> testCases() {
        List<DynamicTest> tests = new ArrayList<>();
        new ClassGraph().acceptPackages("org.pastalab.fray.test").scan().getSubclasses(Object.class.getName()).forEach((classInfo) -> {
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
