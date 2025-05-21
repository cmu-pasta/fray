package org.pastalab.fray.test.success.time;

import java.time.Instant;

public class TestTime {
    public static void main(String[] args) {
        long t1 = System.nanoTime();
        long t2 = System.nanoTime();
        assert(t2 - t1 == 10000L * 1000000L);

        long t3 = System.currentTimeMillis();
        long t4 = System.currentTimeMillis();
        assert(t4 - t3 == 10000);

        Instant t5 = Instant.now();
        Instant t6 = Instant.now();
        assert(t6.toEpochMilli() - t5.toEpochMilli() == 10000);
    }
}
