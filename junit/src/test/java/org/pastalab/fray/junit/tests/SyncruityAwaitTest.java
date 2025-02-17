package org.pastalab.fray.junit.tests;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;

public class SyncruityAwaitTest {

    @Test
    public void testSyncurityAwaitTest() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(org.pastalab.fray.junit.internal.SyncurityAwaitTest.class))
                .execute()
                .allEvents()
                .assertThatEvents()
                .haveExactly(100,
                        event(test("testConstraintSatisfied"), finishedSuccessfully())
                )
                .haveExactly(1,
                        event(test("testConstraintDeadlock"), finishedWithFailure())
                )
                .haveExactly(1,
                        event(test("testInThreadConstraintDeadlock"), finishedWithFailure())
                );
    }
}
