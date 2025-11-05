package org.pastalab.fray.junit.tests.time;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.test;

public class SleepTest {
    @Test
    public void runTests() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(org.pastalab.fray.junit.internal.time.SleepTests.class))
                .execute()
                .allEvents()
                .assertThatEvents()
                .haveExactly(1000,
                        event(test("testSleepInterrupt"), finishedSuccessfully())
                );
    }
}
