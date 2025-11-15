package org.pastalab.fray.junit.internal.thread;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.core.command.SystemTimeDelegateType;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@ExtendWith(FrayTestExtension.class)
public class ForkJoinPoolTests {
    @ConcurrencyTest(
            ignoreTimedBlock = false,
            systemTimeDelegateType = SystemTimeDelegateType.MOCK
    )
    public void testForkJoinPoolAllThreadsRegistered() {
        ForkJoinPool pool = new ForkJoinPool();
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        for (int i = 0; i < 3; i++) {
            pool.execute(() -> {
                try {

                    lock.lock();
                    try {
                        condition.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } finally {
                    lock.unlock();
                }
            });
        }
        pool.shutdown();
    }

    @ConcurrencyTest(
            ignoreTimedBlock = false,
            systemTimeDelegateType = SystemTimeDelegateType.MOCK
    )
    public void testForkJoinPoolCountDownLatchBlock() throws InterruptedException {
        ForkJoinPool pool = new ForkJoinPool();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        for (int i = 0; i < 3; i++) {
            pool.execute(() -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        Thread.sleep(1000);
        countDownLatch.countDown();
        pool.shutdown();
    }
}
