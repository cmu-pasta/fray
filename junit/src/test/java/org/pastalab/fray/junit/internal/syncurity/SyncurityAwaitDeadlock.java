package org.pastalab.fray.junit.internal.syncurity;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.core.scheduler.RandomScheduler;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.equalTo;
import static org.pastalab.fray.junit.syncurity.ConditionFactoryKt.await;

@ExtendWith(FrayTestExtension.class)
public class SyncurityAwaitDeadlock {
    public static class MyThread extends Thread {
        public AtomicBoolean flag = new AtomicBoolean(false);
        public void run() {
            setFlag(true);
        }
        public synchronized boolean getFlag() {
            return flag.get();
        }
        public synchronized void setFlag(boolean value) {
            flag.set(value);
        }
    }

    @ConcurrencyTest(
            scheduler = RandomScheduler.class
    )
    public void testSyncurityAwaitDeadlockInCondition() {
        MyThread t = new MyThread();
        // We need a dummy thread to evaluate the syncurity condition in
        // that thread.
        Thread t2 = new Thread(() -> {
        });
        t2.start();
        t.start();
        await().until(t::getFlag, equalTo(true));
    }
}
