package org.pastalab.fray.junit.tests.scheduler;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

public class ReplaySchedulerTest {
  @Test
  @Disabled
  public void testBankAccountReplay() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(
            selectClass(org.pastalab.fray.junit.internal.scheduler.ReplaySchedulerTest.class))
        .execute()
        .allEvents()
        .assertThatEvents()
        .haveExactly(1, event(test("myBankAccountTest"), finishedWithFailure()));
  }
}
