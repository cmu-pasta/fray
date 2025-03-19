package org.anonlab.fray.junit.internal.syncurity;

import org.junit.jupiter.api.extension.ExtendWith;
import org.anonlab.fray.core.scheduler.RandomScheduler;
import org.anonlab.fray.junit.junit5.FrayTestExtension;
import org.anonlab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.equalTo;
import static org.anonlab.fray.junit.syncurity.ConditionFactoryKt.await;

@ExtendWith(FrayTestExtension.class)
public class SyncurityAwaitDeadlockTest {
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
            scheduler = RandomScheduler.class,
            iterations = 100
    )
    public void testSyncurityAwaitConditionWithSynchronizationPrimitives() {
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
