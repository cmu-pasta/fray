package org.anonlab.fray.test.fail.park;

import java.util.concurrent.locks.LockSupport;

public class ParkDeadlock {
    public static void main(String[] args) {
        LockSupport.park();
    }
}
