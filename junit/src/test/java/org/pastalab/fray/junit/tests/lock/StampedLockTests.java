package org.pastalab.fray.junit.tests.lock;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

public class StampedLockTests {
  @Test
  void testStampedLockSimpleReadWrite() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(org.pastalab.fray.junit.internal.lock.StampedLockTests.class))
        .execute()
        .allEvents()
        .assertThatEvents()
        .haveExactly(1000, event(test("testStampedLockSimpleReadWrite"), finishedSuccessfully()))
        .haveExactly(1, event(test("testStampedLockDeadlock"), finishedWithFailure()));
  }
}
