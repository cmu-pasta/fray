package org.pastalab.fray.junit.tests.lock;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;

public class StampedLockTests {
    @Test
    void testStampedLockSimpleReadWrite() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(org.pastalab.fray.junit.internal.lock.StampedLockTests.class))
                .execute()
                .allEvents()
                .assertThatEvents()
                .haveExactly(1000,
                        event(test("testStampedLockSimpleReadWrite"), finishedSuccessfully())
                )
                .haveExactly(1,
                        event(test("testStampedLockDeadlock"), finishedWithFailure()));
    }
}
