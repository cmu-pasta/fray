package cmu.pasta.sfuzz.it.scheduler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AtomicTest extends IntegrationTestRunner {
    @Test
    public void testInterleaving1() {
        runTest("T1T2");
    }
//
//    @Test
//    public void testInterleaving2() {
//        runTest("T2T1");
//    }
//
//    @Test
//    public void testInterleaving3() {
//        runTest("alternateT2T1");
//    }
}
