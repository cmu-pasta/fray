package org.pastalab.fray.junit.tests.fest;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.test;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

public class FestSchedulerTest {
  @Test
  void launchFraySchedulerTest() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(org.pastalab.fray.junit.internal.fest.FestSchedulerTest.class))
        .execute()
        .allEvents()
        .assertThatEvents()
        .haveExactly(1, event(test("festWithResourceOrderingCoverage"), finishedSuccessfully()))
        .haveExactly(1, event(test("festWithThreadOrderingCoverage"), finishedSuccessfully()))
        .haveExactly(
            1, event(test("festWithNoCoverageFallsBackToDefault"), finishedSuccessfully()));
  }
}
