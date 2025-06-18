package org.pastalab.fray.junit.internal;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

@ExtendWith(FrayTestExtension.class)
public class DummyTest {
    @Test
    public void normalTestFinishedSuccessfully() {
        System.out.println("1");
    }

    @ConcurrencyTest(iterations = 100)
    public void concurrencyTestFinishedSuccessfully() {
        System.out.println("2");
    }

    @ConcurrencyTest(
            iterations = 100
    )
    public void concurrencyTestFinishedWithFailure() {
        assert(false);
    }

    @ConcurrencyTest(
            iterations = 100
    )
    public void concurrencyTestFinishedAllThreadsTerminates() {
        new Thread(() -> {
            while (true) {
                Thread.yield();
            }
        }).start();
        assert (false);
    }
}
