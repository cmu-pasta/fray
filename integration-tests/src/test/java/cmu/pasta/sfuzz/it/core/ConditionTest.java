package cmu.pasta.sfuzz.it.core;

import cmu.pasta.sfuzz.it.IntegrationTestRunner;
import org.junit.jupiter.api.Test;

public class ConditionTest extends IntegrationTestRunner {
    @Test
    public void testInterleaving1() {
        System.out.println(runTest("main"));
    }
}
