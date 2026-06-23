package org.pastalab.fray.junit.plain

import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlinx.serialization.json.Json
import org.pastalab.fray.core.TestRunner
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.command.ExecutionInfo
import org.pastalab.fray.core.command.LambdaExecutor
import org.pastalab.fray.core.command.NetworkDelegateType
import org.pastalab.fray.core.command.SystemTimeDelegateType
import org.pastalab.fray.core.observers.ScheduleVerifier
import org.pastalab.fray.core.randomness.ControlledRandomProvider
import org.pastalab.fray.core.randomness.Randomness
import org.pastalab.fray.core.randomness.RandomnessProvider
import org.pastalab.fray.core.scheduler.POSScheduler
import org.pastalab.fray.core.scheduler.SURWScheduler
import org.pastalab.fray.core.scheduler.Scheduler
import org.pastalab.fray.junit.Common.WORK_DIR

object FrayInTestLauncher {

  fun launchFray(
      runnable: Runnable,
      scheduler: Scheduler,
      randomnessProvider: RandomnessProvider,
      iteration: Int,
      timeout: Int,
      isReplay: Boolean,
      collectTimelineCoverage: org.pastalab.fray.core.observers.TimelineCoverageType =
          org.pastalab.fray.core.observers.TimelineCoverageType.RESOURCE_ORDERING,
      additionalConfigs: (Configuration) -> Unit = { _ -> },
  ) {
    val timelineCoverageType =
        if (collectTimelineCoverage == org.pastalab.fray.core.observers.TimelineCoverageType.NONE)
            null
        else collectTimelineCoverage
    val resolveStackTraceHash = scheduler is SURWScheduler
    val config =
        Configuration(
            ExecutionInfo(
                LambdaExecutor(
                    { runnable.run() },
                ),
                ignoreUnhandledExceptions = false,
                interleaveMemoryOps = false,
                maxScheduledStep = -1,
            ),
            WORK_DIR,
            iteration,
            timeout,
            scheduler,
            randomnessProvider,
            fullSchedule = true,
            exploreMode = false,
            noExitWhenBugFound = true,
            isReplay = isReplay,
            noFray = false,
            dummyRun = false,
            networkDelegateType = NetworkDelegateType.PROACTIVE,
            systemTimeDelegateType = SystemTimeDelegateType.NONE,
            virtualTimeDelta = 100_000L,
            ignoreTimedBlock = true,
            sleepAsYield = true,
            resetClassLoader = true,
            redirectStdout = false,
            abortThreadExecutionAfterMainExit = false,
            resolveRacingOperationStackTraceHash = resolveStackTraceHash,
            timelineCoverageType = timelineCoverageType,
        )
    additionalConfigs(config)
    val runner = TestRunner(config)
    runner.run()?.let { throw it }
  }

  fun launchFrayTest(test: Runnable) {
    launchFray(test, POSScheduler(), ControlledRandomProvider(), 10000, 120, false)
  }

  fun launchFrayReplay(test: Runnable, path: String) {
    // Reconstruct the scheduler from its recorded class name, fed the recorded randomness.
    val className = File("${path}/scheduler").readText().trim()
    val recordedRandom = Json.decodeFromString<Randomness>(File("${path}/random.json").readText())
    val scheduler =
        Class.forName(className).getConstructor(Randomness::class.java).newInstance(recordedRandom)
            as Scheduler
    launchFray(test, scheduler, ControlledRandomProvider(), 1, 10000, true) {
      val recording = Path("${path}/recording.json")
      if (recording.exists()) {
        val verifier = ScheduleVerifier(recording.absolutePathString())
        it.scheduleObservers.add(verifier)
      }
    }
  }
}
