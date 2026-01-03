package org.pastalab.fray.junit.tests.files;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.test;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.pastalab.fray.junit.internal.files.SimpleFileChannelNoCrash;

public class FileChannelTests {
  @Test
  public void testFileChannelNoCrash() {
    EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(SimpleFileChannelNoCrash.class))
        .execute()
        .allEvents()
        .assertThatEvents()
        .haveExactly(100, event(test("testFileChannel"), finishedSuccessfully()));
  }
}
