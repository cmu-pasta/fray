package org.pastalab.fray.test.success.wait;

public class WaitWithTimeoutNoDeadlock {
    public static void main(String[] args) {
        Object o = new Object();
        synchronized (o) {
            try {
                o.wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
