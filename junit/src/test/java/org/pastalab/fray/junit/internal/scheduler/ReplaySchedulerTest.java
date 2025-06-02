package org.pastalab.fray.junit.internal.scheduler;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.core.scheduler.POSScheduler;
import org.pastalab.fray.core.scheduler.ReplayScheduler;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(FrayTestExtension.class)
public class ReplaySchedulerTest {
    public static class BankAccount {
        public AtomicInteger balance = new AtomicInteger(1000);
        public void withdraw(int amount) {
            if (balance.get() >= amount) {
                balance.set(balance.get() - amount);
            }
        }
    }

    @ConcurrencyTest(
            scheduler = ReplayScheduler.class,
            replay = "classpath:org/pastalab/fray/junit/internal/scheduler/ReplaySchedulerTest/recording/"
    )
    public void myBankAccountTest() throws InterruptedException {
        BankAccount account = new BankAccount();
        Thread t1 = new Thread(() -> {
            account.withdraw(500);
            assert(account.balance.get() > 0);
        }, "withdraw-1");
        Thread t2 = new Thread(() -> {
            account.withdraw(700);
            assert(account.balance.get() > 0);
        }, "withdraw-2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
