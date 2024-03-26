package cmu.pasta.sfuzz.it.core;

import cmu.pasta.sfuzz.it.IntegrationTestRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CountDownLatchTest extends IntegrationTestRunner {

    @Test
    public void testCountDown() {
        String expected = "[1]: WORKER-1 finished\n" +
                "[2]: WORKER-2 finished\n" +
                "[3]: WORKER-3 finished\n" +
                "[4]: WORKER-4 finished\n" +
                "[0]: Test worker has finished\n";
        assertEquals(expected, runTest("testCountDown"));
    }

    @Test
    public void testAwaitAfterCountDown() {
        String expected = "[0]: Test worker has finished\n";
        assertEquals(expected, runTest("testAwaitAfterCountDown"));
    }
}
