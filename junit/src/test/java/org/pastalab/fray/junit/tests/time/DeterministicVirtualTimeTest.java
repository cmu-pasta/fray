package org.pastalab.fray.junit.tests.time;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.test;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

public class DeterministicVirtualTimeTest {
  @Test
  public void runTests() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(
            selectClass(org.pastalab.fray.junit.internal.time.DeterministicVirtualTimeTest.class))
        .execute()
        .allEvents()
        .assertThatEvents()
        .haveExactly(1000, event(test("testTimeDeterministicIncrement"), finishedSuccessfully()));
  }
}
