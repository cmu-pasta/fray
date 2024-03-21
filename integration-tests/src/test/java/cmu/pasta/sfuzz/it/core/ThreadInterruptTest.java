package cmu.pasta.sfuzz.it.core;


import cmu.pasta.sfuzz.it.IntegrationTestRunner;
import org.junit.jupiter.api.Test;

public class ThreadInterruptTest extends IntegrationTestRunner {
    @Test
    public void testInterrupt() {
        String result = runTest();
    }
}
