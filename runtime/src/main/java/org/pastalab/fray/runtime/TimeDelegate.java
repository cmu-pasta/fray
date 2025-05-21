package org.pastalab.fray.runtime;

import java.time.Instant;

public class TimeDelegate {
    public long onNanoTime() {
        return System.nanoTime();
    }

    public long onCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public Instant onInstantNow() {
        return Instant.now();
    }

}
