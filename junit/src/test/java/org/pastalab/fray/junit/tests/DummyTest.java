package org.pastalab.fray.junit.tests;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

public class DummyTest {
  @Test
  public void testDummyTestInstance() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(org.pastalab.fray.junit.internal.DummyTest.class))
        .execute()
        .allEvents()
        .assertThatEvents()
        .haveExactly(1, event(test("normalTestFinishedSuccessfully"), finishedSuccessfully()))
        .haveExactly(
            100, event(test("concurrencyTestFinishedSuccessfully"), finishedSuccessfully()))
        .haveExactly(1, event(test("concurrencyTestFinishedWithFailure"), finishedWithFailure()))
        .haveExactly(
            1, event(test("concurrencyTestFinishedAllThreadsTerminates"), finishedWithFailure()));
  }
}
