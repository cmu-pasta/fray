package cmu.pasta.fray.it;


import cmu.pasta.fray.core.*;
import cmu.pasta.fray.core.command.Configuration;
import cmu.pasta.fray.core.command.ExecutionInfo;
import cmu.pasta.fray.core.command.LambdaExecutor;
import cmu.pasta.fray.core.logger.JsonLogger;
import cmu.pasta.fray.core.scheduler.FifoScheduler;
import cmu.pasta.fray.core.scheduler.Scheduler;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntegrationTestRunner {
    public String runTest(Function0<Unit> exec) {
        return runTest(exec, new FifoScheduler(), 1);
    }

    public String runTest(Function0<Unit> exec, Scheduler scheduler, int iter) {
        String testName = this.getClass().getSimpleName();
        EventLogger logger = new EventLogger();
        GlobalContext.INSTANCE.getLoggers().add(logger);
        Configuration config = new Configuration(
                new ExecutionInfo(
                        new LambdaExecutor(() -> {
                            exec.invoke();
                            return null;
                        }),
                        false,
                        true,
                        false,
                        10000
                ),
                "/tmp/report",
                iter,
                scheduler,
                true,
                new JsonLogger("/tmp/report", false),
                false,
                false
        );
        TestRunner runner = new TestRunner(config);
        runner.run();
        return logger.sb.toString();
    }

//    public void runTest(String methodName, String testCase) {
//        String testName = this.getClass().getSimpleName();
//        String expectedFile = "expected/" + testName + "_" + testCase + ".txt";
//        String scheduleFile = "schedules/" + testName + "_" + testCase + ".json";
//        String expected = getResourceAsString(expectedFile);
//        ReplayScheduler scheduler = new ReplayScheduler(Schedule.Companion.fromString(getResourceAsString(scheduleFile), true));
//        assertEquals(expected, runTest(methodName, scheduler));
//    }

    public String getResourceAsString(String path) {
        try(InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";

    }
}