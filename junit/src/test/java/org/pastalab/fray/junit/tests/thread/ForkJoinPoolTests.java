package org.pastalab.fray.junit.tests.thread;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.test;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

public class ForkJoinPoolTests {
  @Test
  public void testForkJoinPool() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(org.pastalab.fray.junit.internal.thread.ForkJoinPoolTests.class))
        .execute()
        .allEvents()
        .assertThatEvents()
        .haveExactly(
            1000, event(test("testForkJoinPoolAllThreadsRegistered"), finishedSuccessfully()))
        .haveExactly(1000, event(test("testCommonPoolTerminationOrder"), finishedSuccessfully()))
        .haveExactly(
            1,
            event(
                test("testForkJoinPoolCommonPoolCreatedAfterForkJoinPoolTermination"),
                finishedSuccessfully()))
        .haveExactly(
            1000, event(test("testForkJoinPoolCountDownLatchBlock"), finishedSuccessfully()));
  }
}
