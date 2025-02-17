package org.pastalab.fray.junit.tests;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;

public class DummyTest {
    @Test
    public void testDummyTestInstance() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(org.pastalab.fray.junit.internal.DummyTest.class))
                .execute()
                .allEvents()
                .assertThatEvents()
                .haveExactly(1,
                        event(test("normalTestFinishedSuccessfully"), finishedSuccessfully())
                )
                .haveExactly(100,
                        event(test("concurrencyTestFinishedSuccessfully"), finishedSuccessfully())
                )
                .haveExactly(1,
                        event(test("concurrencyTestFinishedWithFailure"), finishedWithFailure())
                );
    }
}
