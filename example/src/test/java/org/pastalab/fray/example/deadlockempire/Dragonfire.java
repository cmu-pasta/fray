package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.Semaphore;

/**
 * Translation of "Dragonfire" from The Deadlock Empire
 *
 * This is a final boss challenge demonstrating synchronization issues between
 * two threads representing the heads of a dragon.
 *
 * Story: Your nemesis, the Parallel Wizard, supreme sovereign of the Deadlock Empire,
 * sent out the last of his armies. Your soldiers cannot withstand the Megadragon's
 * scorching fire! But the dragon is merely a creature of artifice. If you could find
 * its weakspot, or a critical section, it would be a great victory.
 *
 * WIN CONDITION: Prevent the dragon from breathing fire by exploiting race conditions
 * to cause both heads to enter their critical sections simultaneously.
 */
@ExtendWith(FrayTestExtension.class)
public class Dragonfire extends DeadlockEmpireTestBase {
    private final Object firebreathing = new Object();
    private final Semaphore fireball = new Semaphore(0);
    private volatile int c = 0;

    @ConcurrencyTest
    public void runTest() {
        Thread firebreathingHead = new Thread(() -> {
            while (true) {
                synchronized (firebreathing) {
                    // incinerate_enemies()

                    if (fireball.tryAcquire()) {
                        // Swoosh!
                        // blast_enemies()

                        // Uh... that was tiring.
                        // I'd better rest while I'm vulnerable...
                        if (fireball.tryAcquire()) {
                            if (fireball.tryAcquire()) {
                                criticalSection();
                            }
                        }
                        // Safe now...
                    }

                    c = c - 1;
                    c = c + 1;
                }
            }
        }, "Firebreathing Head");

        Thread rechargingHead = new Thread(() -> {
            // This is stupid.
            // The other head gets all the cool toys,
            // ...and I get stuck recharging.
            while (true) {
                if (c < 2) {
                    // Let's do some damage!
                    fireball.release();
                    c++;
                } else {
                    // I hate being in here.
                    criticalSection();
                }
            }
        }, "Recharging Head");

        firebreathingHead.start();
        rechargingHead.start();
    }
}
