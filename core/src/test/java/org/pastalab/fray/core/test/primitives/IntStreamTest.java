package org.pastalab.fray.core.test.primitives;

import org.junit.jupiter.api.Test;
import org.pastalab.fray.core.command.Rand;
import org.pastalab.fray.core.randomness.ControlledRandom;
import org.pastalab.fray.core.scheduler.POSScheduler;
import org.pastalab.fray.core.test.FrayRunner;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class IntStreamTest extends FrayRunner {

    @Test
    public void test() {
        Throwable result = buildRunner(() -> {
            AtomicInteger x = new AtomicInteger();
            IntStream.range(1, 10).parallel().forEach((i) -> x.compareAndSet(i-1, i+1));
            assert(x.get() == 10);
            return null;
        }, new POSScheduler(new ControlledRandom(new ArrayList<>(), new ArrayList<>(), new Random())),
                1000).run();
        assertNotNull(result);
    }

}
