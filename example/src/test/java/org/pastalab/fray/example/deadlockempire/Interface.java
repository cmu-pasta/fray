package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

@ExtendWith(FrayTestExtension.class)
public class Interface {
    boolean flag = true;

    @ConcurrencyTest
    public void runTest() {
        Thread thread = new Thread(() -> {
            
        })
    }
}
