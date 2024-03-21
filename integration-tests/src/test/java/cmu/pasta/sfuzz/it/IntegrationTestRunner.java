package cmu.pasta.sfuzz.it;


import cmu.pasta.sfuzz.core.Configuration;
import cmu.pasta.sfuzz.core.Fifo;
import cmu.pasta.sfuzz.core.GlobalContext;
import cmu.pasta.sfuzz.core.MainKt;
import cmu.pasta.sfuzz.core.scheduler.FifoScheduler;
import cmu.pasta.sfuzz.core.scheduler.ReplayScheduler;
import cmu.pasta.sfuzz.core.scheduler.Scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntegrationTestRunner {

    public String runTest() {
        return runTest(new FifoScheduler());
    }

    public String runTest(Scheduler scheduler) {
        String testName = this.getClass().getSimpleName();
        EventLogger logger = new EventLogger();
        GlobalContext.INSTANCE.getLoggers().add(logger);
        Configuration config = new Configuration(
                "example." + testName,
                "/tmp/report",
                "",
                1,
                scheduler,
                true
        );
        MainKt.run(config);
        return logger.sb.toString();
    }

    public void runTest(String testCase) {

        String testName = this.getClass().getSimpleName();
        String expectedFile = "expected/" + testName + "_" + testCase + ".txt";
        String scheduleFile = "schedules/" + testName + "_" + testCase + ".json";
        String expected = getResourceAsString(expectedFile);
        ReplayScheduler scheduler = new ReplayScheduler(getResourceAsString(scheduleFile));
        assertEquals(expected, runTest(scheduler));
    }

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