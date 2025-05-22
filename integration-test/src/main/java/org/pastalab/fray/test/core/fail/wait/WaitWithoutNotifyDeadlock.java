package org.pastalab.fray.test.core.fail.wait;

public class WaitWithoutNotifyDeadlock {
    public static void main(String[] args) {
        Object o = new Object();
        try {
            synchronized (o) {
                o.wait();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
