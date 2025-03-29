package org.pastalab.fray.junit.internal.thread;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(FrayTestExtension.class)
public class ShutdownHookTest {

    public ShutdownHookTest() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        }));
    }

    @ConcurrencyTest
    public void testShutdownHookWithFailureInBeforeEach() {
        fail("Intentional failure.");
    }
}
