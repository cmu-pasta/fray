package org.pastalab.fray.test.fail.wait;

public class WaitWithoutNotify {
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
