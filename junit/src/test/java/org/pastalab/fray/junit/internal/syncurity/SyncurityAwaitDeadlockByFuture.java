package org.pastalab.fray.junit.internal.syncurity;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.equalTo;
import static org.pastalab.fray.junit.syncurity.ConditionFactoryKt.await;

@ExtendWith(FrayTestExtension.class)
public class SyncurityAwaitDeadlockByFuture {

    @ConcurrencyTest
    public void syncurityConditionWithFutureAccess() {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.submit(() -> {
            synchronized (this) {}
        });

        await().until(() -> {
            Future<Integer> result = executor.submit(() -> {
                return 2 + 3;
            });
            try {
                return result.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }, equalTo(5));
    }
}
