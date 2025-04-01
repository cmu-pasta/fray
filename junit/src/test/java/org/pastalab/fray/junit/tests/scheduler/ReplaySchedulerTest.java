package org.pastalab.fray.junit.tests.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;

public class ReplaySchedulerTest {
    @Test
    public void testBankAccountReplay() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(org.pastalab.fray.junit.internal.scheduler.ReplaySchedulerTest.class))
                .execute()
                .allEvents()
                .assertThatEvents()
                .haveExactly(1,
                        event(test("myBankAccountTest"), finishedWithFailure())
                );

    }
}
