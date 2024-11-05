package org.pastalab.fray.test.success.condition;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionAwaitTimeoutNoDeadlock {
    public static void main(String[] args) {
        Lock l = new ReentrantLock();
        Condition c = l.newCondition();
        l.lock();
        try {
            boolean result = c.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
            assert(!result);

            boolean result2 = c.awaitUntil(new java.util.Date(System.currentTimeMillis() + 1000));
            assert(!result2);

            long result3 = c.awaitNanos(1000000000);
            assert(result3 < 1000000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            l.unlock();
        }
    }
}
