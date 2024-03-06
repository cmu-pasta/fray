package cmu.pasta.sfuzz.it;

import cmu.pasta.sfuzz.core.Configuration;
import cmu.pasta.sfuzz.core.GlobalContext;
import cmu.pasta.sfuzz.core.MainKt;
import cmu.pasta.sfuzz.core.concurrency.operations.Operation;
import cmu.pasta.sfuzz.core.logger.LoggerBase;
import cmu.pasta.sfuzz.core.runtime.AnalysisResult;
import cmu.pasta.sfuzz.core.scheduler.Choice;
import cmu.pasta.sfuzz.core.scheduler.FifoScheduler;
import cmu.pasta.sfuzz.core.scheduler.ReplayScheduler;
import example.AtomicTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AtomicIT {

    public static class Logger implements LoggerBase {
        StringBuilder sb = new StringBuilder();

        @Override
        public void executionStart() {
        }

        @Override
        public void newOperationScheduled(@NotNull Operation op, @NotNull Choice choice) {
        }

        @Override
        public void executionDone(@NotNull AnalysisResult result) {

        }

        @Override
        public void applicationEvent(@NotNull String event) {
            sb.append(event);
        }
    }
    @Test
    public void testInterleaving1() {
        List<Choice> choiceList = new ArrayList<>();
        choiceList.add(new Choice(0, 0, 2));
        choiceList.add(new Choice(0, 0, 2));
        choiceList.add(new Choice(0, 0, 2));
        choiceList.add(new Choice(0, 0, 2));
        choiceList.add(new Choice(1, 0, 2));
        choiceList.add(new Choice(1, 0, 2));
        choiceList.add(new Choice(1, 0, 2));
        choiceList.add(new Choice(1, 0, 2));

        Logger logger = new Logger();
        GlobalContext.INSTANCE.getLoggers().add(logger);


        Configuration config = new Configuration(
                AtomicTest.class.getName(),
                "/tmp/report",
                "",
                new FifoScheduler()
//                new ReplayScheduler(choiceList)
                );
        MainKt.run(config);


        String expected = "expected/AtomicTest_T1T2.txt";

        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(expected);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            assertEquals(sb.toString(), logger.sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
