package org.pastalab.fray.junit.tests.surw;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.pastalab.fray.junit.internal.surw.TestUniformSamplingLeftShift;

import java.util.Map;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;

public class TestUniformSampling {
    @Test
    public void testUniformSampling() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(org.pastalab.fray.junit.internal.surw.TestUniformSampling.class))
                .execute()
                .allEvents()
                .assertThatEvents()
                .haveExactly(1000,
                        event(test("testUniformSampling"), finishedSuccessfully())
                );
        Map<Integer, Integer> values = org.pastalab.fray.junit.internal.surw.TestUniformSampling.xValues;
        for (Map.Entry<Integer, Integer> entry : values.entrySet()) {
            System.out.println("Value " + entry.getKey() + " returned " + entry.getValue() + " times.");
        }
    }

    @Test
    public void testUniformSamplingLeftShift() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(TestUniformSamplingLeftShift.class))
                .execute()
                .allEvents()
                .assertThatEvents()
                .haveExactly(1000,
                        event(test("testUniformSampling"), finishedSuccessfully())
                );
        Map<Integer, Integer> values = org.pastalab.fray.junit.internal.surw.TestUniformSamplingLeftShift.xValues;
        for (Map.Entry<Integer, Integer> entry : values.entrySet()) {
            System.out.println("Value " + entry.getKey() + " returned " + entry.getValue() + " times.");
        }
    }

}
