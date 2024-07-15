package cmu.pasta.fray.it.core;

import cmu.pasta.fray.core.scheduler.POSScheduler;
import cmu.pasta.fray.it.IntegrationTestRunner;
import example.FrayExample;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FrayExampleTest extends IntegrationTestRunner {

    @Test
    void test() {
        String result = runTest(() -> {
            try {
                FrayExample.main(new String[0]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        }, new POSScheduler(new Random()), 10000);
        assertTrue(result.contains("DeadlockException") || result.contains("AssertionError"));
    }
}
