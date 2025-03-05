package org.pastalab.fray.junit.internal.syncurity;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;
import org.pastalab.fray.junit.syncurity.ConditionFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.pastalab.fray.junit.syncurity.ConditionFactoryKt.await;

@ExtendWith(FrayTestExtension.class)
public class SyncurityAwaitDeadlockInConditionTest {
    @ConcurrencyTest(
            iterations = 100
    )
    public void testConstraintWithPark() {
        await().until(() -> {
            LockSupport.park();
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
