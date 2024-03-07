package cmu.pasta.sfuzz.it;


import cmu.pasta.sfuzz.core.Configuration;
import cmu.pasta.sfuzz.core.GlobalContext;
import cmu.pasta.sfuzz.core.MainKt;
import cmu.pasta.sfuzz.core.scheduler.FifoScheduler;
import cmu.pasta.sfuzz.core.scheduler.ReplayScheduler;
import cmu.pasta.sfuzz.core.scheduler.Schedule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntegrationTestRunner {

    public void runTest(String testCase) {

        String testName = this.getClass().getSimpleName();
        String expectedFile = "expected/" + testName + "_" + testCase + ".txt";
        String scheduleFile = "schedules/" + testName + "_" + testCase + ".json";
        String expected = getResourceAsString(expectedFile);
        ReplayScheduler scheduler = new ReplayScheduler(getResourceAsString(scheduleFile));

        EventLogger logger = new EventLogger();
        GlobalContext.INSTANCE.getLoggers().add(logger);
        Configuration config = new Configuration(
                "example." + testName,
                "/tmp/report",
                "",
                scheduler,
//                new FifoScheduler(),
                true
        );

        MainKt.run(config);

        assertEquals(expected, logger.sb.toString());
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