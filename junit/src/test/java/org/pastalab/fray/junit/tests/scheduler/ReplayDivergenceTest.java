package org.pastalab.fray.junit.tests.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

public class ReplayDivergenceTest {
  @Test
  public void testReplayDivergence() {
    EngineExecutionResults results =
        EngineTestKit.engine("junit-jupiter")
            .selectors(
                selectClass(org.pastalab.fray.junit.internal.scheduler.ReplayDivergenceTest.class))
            .execute();

    results
        .allEvents()
        .assertThatEvents()
        .haveExactly(1, event(test("replayDiverges"), finishedWithFailure()))
        .haveExactly(1, event(test("replayMatches"), finishedSuccessfully()));

    Throwable failure =
        results
            .testEvents()
            .failed()
            .list()
            .get(0)
            .getRequiredPayload(TestExecutionResult.class)
            .getThrowable()
            .orElseThrow();
    assertInstanceOf(IllegalStateException.class, failure);
    assertEquals(
        "Replay diverged at recording index 0: scheduled thread 9999 is not runnable; runnable threads: [0]",
        failure.getMessage());
  }
}
