package cmu.pasta.sfuzz.it.core;

import cmu.pasta.sfuzz.it.IntegrationTestRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
public class SemaphoreTest extends IntegrationTestRunner {
    @Test
    public void testSemaphore() {
        String event = "[1]: Starting A\n" +
                "[1]: A is waiting for a permit.\n" +
                "[1]: A gets a permit.\n" +
                "[1]: A: 1\n" +
                "[1]: A: 2\n" +
                "[1]: A: 3\n" +
                "[1]: A: 4\n" +
                "[1]: A: 5\n" +
                "[1]: A releases the permit.\n" +
                "[2]: Starting B\n" +
                "[2]: B is waiting for a permit.\n" +
                "[2]: B gets a permit.\n" +
                "[2]: B: 4\n" +
                "[2]: B: 3\n" +
                "[2]: B: 2\n" +
                "[2]: B: 1\n" +
                "[2]: B: 0\n" +
                "[2]: B releases the permit.\n" +
                "[0]: count: 0\n";
        assertEquals(event, runTest("testSemaphore"));
    }
}
