package cmu.pasta.sfuzz.it.core;

import cmu.pasta.sfuzz.it.IntegrationTestRunner;
import org.junit.jupiter.api.Test;

public class ReentrantReadWriteLockTest extends IntegrationTestRunner {
    @Test
    public void test() {
        runTest("test");
    }
}
