package org.pastalab.fray.example.deadlockempire;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Translation of "Triple Danger" from The Deadlock Empire
 *
 * This challenge demonstrates synchronization issues with a shared queue
 * and multiple consumers.
 *
 * Story: A new enemy appeared - a sorcerer riding a fearsome megadragon.
 * The sorcerer calls upon magical energy from the lands and feeds it to the dragon.
 * Energized by the sorcerer's mana, the dragon launches fireballs and lightning bolts,
 * devastating the Sequentialist army.
 *
 * WIN CONDITION: Cause race conditions or exceptions between the three threads
 * by exploiting the shared queue and monitor.
 */
@ExtendWith(FrayTestExtension.class)
public class TripleDanger extends DeadlockEmpireTestBase {
    private final Object conduit = new Object();
    private final Queue<EnergyBurst> energyBursts = new LinkedList<>();

    // Simple placeholder class for energy bursts
    private static class EnergyBurst {}

    @ConcurrencyTest
    public void runTest() {
        Thread sorcerer = new Thread(() -> {
            while (true) {
                synchronized (conduit) {
                    // I summon mana for you, dragon!
                    // Incinerate the enemies!
                    energyBursts.add(new EnergyBurst());
                }
            }
        }, "Sorcerer");

        Thread electricityHead = new Thread(() -> {
            while (true) {
                if (!energyBursts.isEmpty()) {
                    synchronized (conduit) {
                        // This might throw NoSuchElementException if the queue is empty
                        EnergyBurst burst = energyBursts.remove();

                        // lightning_bolts(terrifying: true)
                    }
                }
            }
        }, "Dragon Head (Electricity)");

        Thread fireHead = new Thread(() -> {
            while (true) {
                if (!energyBursts.isEmpty()) {
                    synchronized (conduit) {
                        // This might throw NoSuchElementException if the queue is empty
                        EnergyBurst burst = energyBursts.remove();

                        // fireball(mighty: true)
                    }
                }
            }
        }, "Dragon Head (Fire)");

        sorcerer.start();
        electricityHead.start();
        fireHead.start();
    }
}
