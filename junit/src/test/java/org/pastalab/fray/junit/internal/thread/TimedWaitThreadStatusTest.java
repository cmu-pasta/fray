package org.pastalab.fray.junit.internal.thread;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.core.command.SystemTimeDelegateType;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(FrayTestExtension.class)
public class TimedWaitThreadStatusTest {

    @ConcurrencyTest(
            iterations = 100,
            sleepAsYield = false,
            systemTimeDelegateType = SystemTimeDelegateType.MOCK,
            ignoreTimedBlock = false
    )
    public void testTimedWaitRightThreadStatus() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            try {
                latch.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
        Thread.sleep(100);
        Thread.State state = thread.getState();
        assertEquals(Thread.State.TIMED_WAITING, state, "Thread is not in WAITING state");
        thread.join();
    }
}
