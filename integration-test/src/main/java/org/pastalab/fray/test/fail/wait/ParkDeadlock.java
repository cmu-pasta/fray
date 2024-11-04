package org.pastalab.fray.test.fail.wait;

import java.util.concurrent.locks.LockSupport;

public class ParkDeadlock {
    public static void main(String[] args) {
        LockSupport.park();
    }
}
