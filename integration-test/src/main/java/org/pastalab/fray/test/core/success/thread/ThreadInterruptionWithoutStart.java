package org.pastalab.fray.test.core.success.thread;

public class ThreadInterruptionWithoutStart {

    public static void main(String[] args) {
        Thread t = new Thread();
        t.interrupt();
    }
}
