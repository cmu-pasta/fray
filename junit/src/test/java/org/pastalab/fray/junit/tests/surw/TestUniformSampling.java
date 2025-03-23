package org.pastalab.fray.junit.tests.surw;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.pastalab.fray.junit.internal.surw.TestUniformSamplingLeftShift;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;

public class TestUniformSampling {
    public double calculateStandardDeviation(List<Integer> numbers) {
        double mean = numbers.stream()
                .mapToDouble(Integer::doubleValue)
                .average()
                .orElse(0.0);
        double sumOfSquaredDifferences = numbers.stream()
                .mapToDouble(num -> Math.pow(num - mean, 2))
                .sum();

        double variance = sumOfSquaredDifferences / numbers.size();
        return Math.sqrt(variance);
    }

    // We need to disable these tests until we find a more reliable way to test them.
    @Test
    @Disabled
    public void testUniformSampling() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(org.pastalab.fray.junit.internal.surw.TestUniformSampling.class))
                .execute()
                .allEvents()
                .assertThatEvents()
                .haveExactly(5000,
                        event(test("testUniformSampling"), finishedSuccessfully())
                );
        Map<Integer, Integer> values = org.pastalab.fray.junit.internal.surw.TestUniformSampling.xValues;
        Assertions.assertTrue(calculateStandardDeviation(new ArrayList(values.values())) < 50);
    }

    // We disable this test because Fray currently considers both read and write operations
    // are interesting. This is different fromt the original test.
    @Disabled
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
