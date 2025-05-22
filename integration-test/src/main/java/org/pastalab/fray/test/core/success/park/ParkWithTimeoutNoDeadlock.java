package org.pastalab.fray.test.core.success.park;

import java.util.concurrent.locks.LockSupport;

public class ParkWithTimeoutNoDeadlock {
    public static void main(String[] args) {
        LockSupport.parkNanos(1000000000);
        LockSupport.parkUntil(System.currentTimeMillis() + 1000);
        LockSupport.parkUntil(null, System.currentTimeMillis() + 1000);
        LockSupport.parkNanos(null, 1000000000);
    }
}
