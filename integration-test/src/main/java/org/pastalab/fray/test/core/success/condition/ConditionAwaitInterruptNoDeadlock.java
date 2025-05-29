package org.pastalab.fray.test.core.success.condition;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionAwaitInterruptNoDeadlock {
    public static void main(String[] args) {
        Lock l = new ReentrantLock();
        Condition c = l.newCondition();
        Thread t = new Thread(() -> {
            l.lock();
            try {
                Thread thread = Thread.currentThread();
                new Thread(() -> {
                    l.lock();
                    c.signalAll();
                    l.unlock();
                    thread.interrupt();
                }).start();
                c.await();
            } catch (InterruptedException e) {
            }
            l.unlock();
        });
        t.start();
    }
}
