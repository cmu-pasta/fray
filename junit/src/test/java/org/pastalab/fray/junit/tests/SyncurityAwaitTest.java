package org.anonlab.fray.junit.tests;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.anonlab.fray.junit.internal.syncurity.SyncurityAwaitDeadlockInConditionTest;
import org.anonlab.fray.junit.internal.syncurity.SyncurityAwaitDeadlockTest;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;

public class SyncurityAwaitTest {

    @Test
    public void testSyncurityAwaitTest() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(org.anonlab.fray.junit.internal.syncurity.SyncurityAwaitTest.class))
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

    @Test
    public void testSyncurityAwaitWithSynchronizationPrimitives() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(SyncurityAwaitDeadlockTest.class))
                .execute()
                .allEvents()
                .assertThatEvents()
                .haveExactly(100,
                        event(test("testSyncurityAwaitConditionWithSynchronizationPrimitives"), finishedSuccessfully())
                );
    }

    @Test
    public void testSyncurityAwaitDeadlockInCondition() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(SyncurityAwaitDeadlockInConditionTest.class))
                .execute()
                .allEvents()
                .assertThatEvents()
                .haveExactly(1,
                        event(test("testConstraintWithPark"), finishedWithFailure())
                        )
                .haveExactly(1,
                        event(test("testConstraintWithWait"), finishedWithFailure())
                        );
    }

}
