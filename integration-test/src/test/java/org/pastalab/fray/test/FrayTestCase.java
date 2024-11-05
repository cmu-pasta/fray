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
import org.pastalab.fray.core.observers.ScheduleRecording;
import org.pastalab.fray.core.observers.ScheduleVerifier;
import org.pastalab.fray.core.randomness.ControlledRandom;
import org.pastalab.fray.core.scheduler.FifoScheduler;
import org.pastalab.fray.core.scheduler.POSScheduler;
import org.pastalab.fray.core.scheduler.RandomScheduler;
import org.pastalab.fray.runtime.TargetTerminateException;
import org.pastalab.fray.test.success.condition.ConditionAwaitTimeoutInterrupt;
import org.pastalab.fray.test.success.condition.ConditionAwaitTimeoutNotifyInterrupt;

import java.io.File;
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
                        true,
                        false,
                        -1
                ),
                "/tmp/report",
                10000,
                60,
                new RandomScheduler(),
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
                            ConditionAwaitTimeoutNotifyInterrupt.main(new String[]{});
                            return null;
                        }),
                        false,
                        true,
                        false,
                        -1
                ),
                "/tmp/report2",
                100,
                60,
                new RandomScheduler(new ControlledRandom(
                        new ArrayList<>(List.of(1, 1, 1, 0)),
                        new ArrayList<>(),
                        new Random()
                )),
                new ControlledRandom(
                        new ArrayList<>(List.of(0, 0, 0, 0, 0, 0)),
                        new ArrayList<>(),
                        new Random()
                ),
                true,
                false,
                true,
                false,
                false,
                false
        );
//        config.getScheduleObservers().add(new ScheduleVerifier("/tmp/report/recording_0/recording.json"));
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
