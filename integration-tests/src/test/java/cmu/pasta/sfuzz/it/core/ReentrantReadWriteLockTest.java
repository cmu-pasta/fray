package cmu.pasta.sfuzz.it.core;

import cmu.pasta.sfuzz.it.IntegrationTestRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
public class ReentrantReadWriteLockTest extends IntegrationTestRunner {
    @Test
    public void test() {
        String s = runTest("test");
        assertEquals("[3]: ReadThread Message is ab\n", s);
    }

    @Test
    public void testInterrupt() {
        String s = runTest("testInterrupt");
        assertEquals("[1]: Thread 1 trying to acquire write lock\n" +
                "[1]: Thread 1 interrupted\n", s);
    }

    @Test
    public void testInterruptAfterAcquire() {
        String s = runTest("testInterruptAfterAcquire");
        assertEquals("[1]: Thread 1 acquired write lock\n" +
                "[0]: Thread 1 is interrupted\n" +
                "[1]: Thread 1 interrupted\n", s);
    }
}
