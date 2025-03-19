package org.anonlab.fray.test.fail.intstream;

import org.anonlab.fray.test.ExpectedException;

import java.util.concurrent.atomic.AtomicInteger;

public class IntStream {
    public static void main(String[] args) {
        AtomicInteger x = new AtomicInteger();
        java.util.stream.IntStream.range(1, 10).parallel().forEach((i) -> x.compareAndSet(i-1, i+1));
        if (x.get() != 10) {
            throw new ExpectedException("x (" + x.get() + ") is not 10");
        }
    }
}
