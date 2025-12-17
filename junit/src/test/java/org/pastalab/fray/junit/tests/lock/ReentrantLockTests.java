package org.pastalab.fray.junit.tests.lock;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;

public class ReentrantLockTests {

    @Test
    void launchReentrantLockTests() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(org.pastalab.fray.junit.internal.lock.ReentrantLockTests.class))
                .execute()
                .allEvents()
                .assertThatEvents()
                .haveExactly(1,
                        event(test("testReentrantLockUnlockWithoutLock"), finishedSuccessfully())
                )
                .haveExactly(1,
                        event(test("testReadLockUnlockWithoutLock"), finishedSuccessfully())
                )
                .haveExactly(1,
                        event(test("testWriteLockUnlockWithoutLock"), finishedSuccessfully())
                );
    }
}
