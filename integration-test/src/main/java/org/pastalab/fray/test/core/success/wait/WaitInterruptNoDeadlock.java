package org.pastalab.fray.test.core.success.wait;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WaitInterruptNoDeadlock {
    public static void main(String[] args) {
        Object o = new Object();
        Thread t = new Thread(() -> {
            synchronized (o) {
                try {
                    Thread thread = Thread.currentThread();
                    new Thread(() -> {
                        synchronized (o) {
                            o.notify();
                        }
                        thread.interrupt();
                    }).start();
                    o.wait();
                } catch (InterruptedException e) {
                }
            }
        });
        t.start();
    }
}
