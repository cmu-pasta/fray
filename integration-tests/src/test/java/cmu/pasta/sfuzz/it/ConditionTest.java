package cmu.pasta.sfuzz.it;

import org.junit.jupiter.api.Test;

public class ConditionTest extends IntegrationTestRunner {
    @Test
    public void testInterleaving1() {
        runTest("T1T2");
    }
}
