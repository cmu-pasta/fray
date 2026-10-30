package org.pastalab.fray.junit.internal.scheduler;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.core.scheduler.ReplayScheduler;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.FrayTest;

@ExtendWith(FrayTestExtension.class)
public class ReplayDivergenceTest {
  @FrayTest(
      scheduler = ReplayScheduler.class,
      replay =
          "classpath:org/pastalab/fray/junit/internal/scheduler/ReplayDivergenceTest/diverged/")
  public void replayDiverges() {}

  @FrayTest(
      scheduler = ReplayScheduler.class,
      replay = "classpath:org/pastalab/fray/junit/internal/scheduler/ReplayDivergenceTest/matched/")
  public void replayMatches() {}
}
