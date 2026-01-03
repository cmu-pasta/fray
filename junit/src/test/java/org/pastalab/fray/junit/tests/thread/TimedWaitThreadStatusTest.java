package org.pastalab.fray.junit.tests.thread;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.test;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

public class TimedWaitThreadStatusTest {
  @Test
  public void testTimedWaitRightThreadStatus() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(
            selectClass(org.pastalab.fray.junit.internal.thread.TimedWaitThreadStatusTest.class))
        .execute()
        .allEvents()
        .assertThatEvents()
        .haveExactly(100, event(test("testTimedWaitRightThreadStatus"), finishedSuccessfully()));
  }
}
