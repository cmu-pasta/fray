package org.pastalab.fray.core.scheduler

import java.nio.file.Files
import java.nio.file.Path
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.command.ExecutionInfo
import org.pastalab.fray.core.command.LambdaExecutor
import org.pastalab.fray.core.command.NetworkDelegateType
import org.pastalab.fray.core.command.SystemTimeDelegateType
import org.pastalab.fray.core.observers.ResourceOrderingCoverage
import org.pastalab.fray.core.observers.ThreadOrderingCoverage
import org.pastalab.fray.core.observers.TimelineCoverage
import org.pastalab.fray.core.observers.TimelineCoverageType
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.randomness.ControlledRandomProvider

/** A controllable [TimelineCoverage] whose cumulative coverage count is set by the test. */
private class StubCoverage : TimelineCoverage {
  var coverageCount: Int = 0

  override fun getCoverage(): Int = coverageCount

  override fun onExecutionStart() {}

  override fun onNewSchedule(allThreads: Collection<ThreadContext>, scheduled: ThreadContext) {}

  override fun onContextSwitch(current: ThreadContext, next: ThreadContext) {}

  override fun onExecutionDone(bugFound: Throwable?) {}

  override fun onReportError(throwable: Throwable) {}

  override fun saveToReportFolder(path: Path) {}
}

/**
 * Runs one Fest iteration transition; coverage is read from the (carried-forward) [coverage] field.
 */
private fun FestScheduler.step(controller: ControlledRandom = ControlledRandom()): FestScheduler =
    nextIteration(controller) as FestScheduler

class FestSchedulerTest {

  // ---------------------------------------------------------------------------------------------
  // FestRecording: capture / replay / mutate
  // ---------------------------------------------------------------------------------------------

  @Test
  fun replayReproducesRecordedStream() {
    val recording = FestRecording(listOf(7, 8, 9), listOf(0.1, 0.2))
    val rand = recording.toRandom()
    assertEquals(7, rand.nextInt())
    assertEquals(8, rand.nextInt())
    assertEquals(0.1, rand.nextDouble())
  }

  @Test
  fun captureSnapshotsConsumedValues() {
    val rand = ControlledRandom(mutableListOf(1, 2, 3), mutableListOf(0.7))
    val recording = FestRecording.capture(rand)
    assertEquals(listOf(1, 2, 3), recording.integers)
    assertEquals(listOf(0.7), recording.doubles)
  }

  @Test
  fun mutateChangesAtMostMutationCountValuesAndPreservesLength() {
    val original = FestRecording((0 until 100).toList(), emptyList())
    val mutant = original.mutate(Random(1234), 5)
    assertEquals(100, mutant.integers.size)
    val diffs = (0 until 100).count { original.integers[it] != mutant.integers[it] }
    // At most 5 positions change (a mutation may re-pick the same position or value).
    assertTrue(diffs in 0..5, "expected at most 5 changed positions, got $diffs")
  }

  @Test
  fun mutateOfEmptyRecordingIsNoop() {
    val empty = FestRecording(emptyList(), emptyList())
    val mutant = empty.mutate(Random(1), 5)
    assertEquals(0, mutant.integers.size)
  }

  // ---------------------------------------------------------------------------------------------
  // FestScheduler.nextIteration: the coverage-guided feedback loop
  // ---------------------------------------------------------------------------------------------

  @Test
  fun seedExecutionIsAlwaysAddedToCorpus() {
    val coverage = StubCoverage().apply { coverageCount = 1 }
    val fest =
        FestScheduler(RandomScheduler(ControlledRandom(mutableListOf(5, 6), mutableListOf())))
    fest.coverage = coverage

    val next = fest.step()

    // The seed recording is always kept, and a parent is checked out for mutation.
    assertEquals(1, next.exploration.corpus.size)
    assertEquals(1, next.exploration.lastCoverage)
  }

  @Test
  fun recordingIsKeptOnlyWhenCoverageIncreases() {
    val coverage = StubCoverage()
    val fest = FestScheduler(RandomScheduler(ControlledRandom(mutableListOf(1), mutableListOf())))
    fest.coverage = coverage

    // Iteration 0 (seed): coverage 1 -> corpus has the seed.
    coverage.coverageCount = 1
    val afterSeed = fest.step()
    assertEquals(1, afterSeed.exploration.corpus.size)

    // Iteration 1: coverage unchanged (still 1) -> recording NOT kept.
    coverage.coverageCount = 1
    val afterNoGain = afterSeed.step()
    assertEquals(1, afterNoGain.exploration.corpus.size)

    // Iteration 2: coverage increased to 2 -> recording kept.
    coverage.coverageCount = 2
    val afterGain = afterNoGain.step()
    assertEquals(2, afterGain.exploration.corpus.size)
  }

  @Test
  fun explorationStateIsSharedAcrossIterations() {
    val coverage = StubCoverage().apply { coverageCount = 1 }
    val fest = FestScheduler(RandomScheduler(ControlledRandom(mutableListOf(1), mutableListOf())))
    fest.coverage = coverage
    val next = fest.step()
    val nextNext = next.step()
    // The same exploration object (corpus) is carried forward by reference.
    assertTrue(next.exploration === nextNext.exploration)
  }

  // ---------------------------------------------------------------------------------------------
  // Default coverage: Fest needs a feedback signal, so Configuration provides one when none is set.
  // ---------------------------------------------------------------------------------------------

  private fun buildConfig(
      scheduler: Scheduler,
      coverageType: TimelineCoverageType?,
  ): Configuration =
      Configuration(
          ExecutionInfo(LambdaExecutor({}), false, false, -1),
          Files.createTempDirectory("fray-fest-test"),
          1,
          1000,
          scheduler,
          ControlledRandomProvider(),
          fullSchedule = false,
          exploreMode = false,
          noExitWhenBugFound = true,
          isReplay = false,
          noFray = false,
          dummyRun = false,
          networkDelegateType = NetworkDelegateType.NONE,
          systemTimeDelegateType = SystemTimeDelegateType.NONE,
          virtualTimeDelta = 100_000L,
          ignoreTimedBlock = true,
          sleepAsYield = false,
          resetClassLoader = true,
          redirectStdout = false,
          abortThreadExecutionAfterMainExit = false,
          resolveRacingOperationStackTraceHash = false,
          timelineCoverageType = coverageType,
      )

  @Test
  fun festFallsBackToDefaultCoverageWhenNoneSpecified() {
    for (coverageType in listOf(null, TimelineCoverageType.NONE)) {
      val config = buildConfig(FestScheduler(), coverageType)
      // Fest must have a coverage signal, so a default (resource-ordering) is provided...
      assertTrue(config.timelineCoverage is ResourceOrderingCoverage)
      // ...registered as an observer so it actually measures, and wired into the scheduler.
      assertTrue(config.scheduleObservers.contains(config.timelineCoverage))
      assertEquals(config.timelineCoverage, (config.scheduler as FestScheduler).coverage)
    }
  }

  @Test
  fun festHonorsAnExplicitCoverageChoice() {
    val config = buildConfig(FestScheduler(), TimelineCoverageType.THREAD_ORDERING)
    assertTrue(config.timelineCoverage is ThreadOrderingCoverage)
  }

  @Test
  fun nonFestSchedulerKeepsNoCoverageByDefault() {
    val config = buildConfig(RandomScheduler(), TimelineCoverageType.NONE)
    assertNull(config.timelineCoverage)
  }

  @Test
  fun mutationBudgetIsSpentBeforeAdvancingToNextParent() {
    val coverage = StubCoverage().apply { coverageCount = 1 }
    // Budget of 2: a checked-out parent is mutated twice before the cursor advances.
    val fest =
        FestScheduler(
            RandomScheduler(ControlledRandom(mutableListOf(1), mutableListOf())),
            mutationCount = 1,
            mutationBudget = 2,
        )
    fest.coverage = coverage
    val s1 = fest.step()
    assertEquals(1, s1.exploration.parentCursor)
    // No new coverage, so the corpus stays at one entry and the same parent keeps being mutated.
    val s2 = s1.step()
    assertEquals(1, s2.exploration.parentCursor)
    // Budget (2) now exhausted -> cursor advances when the next parent is checked out.
    val s3 = s2.step()
    assertEquals(2, s3.exploration.parentCursor)
  }
}
