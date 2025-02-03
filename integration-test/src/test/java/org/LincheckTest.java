package org;

import org.junit.jupiter.api.Test;
import org.pastalab.fray.test.fail.wait.NotifyOrder;
import org.jetbrains.kotlinx.lincheck.LincheckKt;

public class LincheckTest {

    @Test
    public void test() throws InterruptedException {
        LincheckKt.runConcurrentTest(100000, () -> {
            try {
                NotifyOrder.main(null);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }
}
