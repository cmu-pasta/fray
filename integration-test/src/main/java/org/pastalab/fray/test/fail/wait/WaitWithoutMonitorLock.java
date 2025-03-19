package org.anonlab.fray.test.fail.wait;

import org.anonlab.fray.test.ExpectedException;

public class WaitWithoutMonitorLock {
    public static void main(String[] args) {
        Object o = new Object();
        try {
            o.wait();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IllegalMonitorStateException e) {
            throw new ExpectedException(e);
        }
    }
}
