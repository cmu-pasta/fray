package cmu.pasta.sfuzz.it;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AtomicTest extends IntegrationTestRunner {
    @Test
    public void testInterleaving1() {
        runTest("T1T2");
    }
}
