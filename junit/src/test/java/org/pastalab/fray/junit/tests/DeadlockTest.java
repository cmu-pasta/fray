package org.pastalab.fray.junit.tests;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;

public class DeadlockTest {
    @Test
    void verifyBothTestsAreFailed() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(org.pastalab.fray.junit.internal.DeadlockTest.class))
                .execute()
                .allEvents()
                .assertThatEvents()
                .haveExactly(1,
                        event(test("deadlockInChildThread"), finishedWithFailure())
                )
                .haveExactly(1,
                        event(test("deadlockInMainThread"), finishedWithFailure())
                );
    }
}

