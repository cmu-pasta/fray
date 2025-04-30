package org.pastalab.fray.junit.internal.syncurity;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.pastalab.fray.junit.ranger.ConditionFactoryKt.await;

@ExtendWith(FrayTestExtension.class)
public class RangerAwaitDeadlockInConditionTest {
    @ConcurrencyTest(
            iterations = 100
    )
    public void testConstraintWithPark() {
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        await().until(() -> {
            lock.lock();
            try {
                condition.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock.unlock();
            return 1;
        }, equalTo(1));
    }

    @ConcurrencyTest(
            iterations = 100
    )
    public void testConstraintWithWait() {
        Object o = new Object();
        await().until(() -> {
            synchronized (o) {
                try {
                    o.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return 1;
        }, equalTo(1));
    }
}
