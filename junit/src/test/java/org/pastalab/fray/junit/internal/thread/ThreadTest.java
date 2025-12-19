package org.pastalab.fray.junit.internal.thread;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.core.scheduler.RandomScheduler;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.FrayTest;

@ExtendWith(FrayTestExtension.class)
public class ThreadTest {

    @FrayTest(
            scheduler = RandomScheduler.class
    )
    void testThreadSetNameNoDeadlock() throws InterruptedException {
        Thread t = new Thread(() -> {
            Thread.currentThread().setName("TestThread");
        });
        t.start();
        t.join();
    }

}
