package org.pastalab.fray.junit.tests;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.pastalab.fray.junit.internal.syncurity.SyncurityAwaitDeadlockInConditionTest;
import org.pastalab.fray.junit.internal.syncurity.SyncurityAwaitDeadlockTest;
import org.pastalab.fray.junit.internal.syncurity.SyncurityAwaitTest;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;

public class SyncruityAwaitTest {

    @Test
    public void testSyncurityAwaitTest() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(SyncurityAwaitTest.class))
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
                );
    }

}
