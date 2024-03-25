package cmu.pasta.sfuzz.it.core;


import cmu.pasta.sfuzz.it.IntegrationTestRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThreadInterruptTest extends IntegrationTestRunner {
    @Test
    public void testInterruptBeforeWait() {
        assertEquals("[1]: Interrupted\n", runTest("testInterruptBeforeWait"));
    }

    @Test
    public void testInterruptDuringWait() {
        assertEquals("[1]: Interrupted\n", runTest("testInterruptDuringWait"));
    }

    @Test
    public void testInterruptCleared() {
        runTest("testInterruptCleared");
    }
}
