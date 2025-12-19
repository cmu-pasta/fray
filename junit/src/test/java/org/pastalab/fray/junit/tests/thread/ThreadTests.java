package org.pastalab.fray.junit.tests.thread;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;

public class ThreadTests {

    @Test
    void runThreadTests() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(org.pastalab.fray.junit.internal.thread.ThreadTest.class))
                .execute()
                .allEvents()
                .assertThatEvents()
                .haveExactly(1,
                        event(test("testThreadSetNameNoDeadlock"), finishedSuccessfully())
                );
    }
}
