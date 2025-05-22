package org.pastalab.fray.test.core.fail.wait;

import org.pastalab.fray.test.ExpectedException;

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
