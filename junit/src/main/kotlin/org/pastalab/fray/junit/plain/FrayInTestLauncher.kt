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
import org.pastalab.fray.core.randomness.RandomnessProvider
import org.pastalab.fray.core.randomness.RecordedRandomProvider
import org.pastalab.fray.core.scheduler.POSScheduler
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
      additionalConfigs: (Configuration) -> Unit = { _ -> },
  ) {
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
            WORK_DIR.absolutePathString(),
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
            ignoreTimedBlock = true,
            sleepAsYield = true,
        )
    additionalConfigs(config)
    val runner = TestRunner(config)
    runner.run()?.let { throw it }
  }

  fun launchFrayTest(test: Runnable) {
    launchFray(test, POSScheduler(), ControlledRandomProvider(), 10000, 120, false)
  }

  fun launchFrayReplay(test: Runnable, path: String) {
    val randomPath = "${path}/random.json"
    val schedulerPath = "${path}/schedule.json"
    val randomnessProvider = RecordedRandomProvider(randomPath)
    val scheduler = Json.decodeFromString<Scheduler>(File(schedulerPath).readText())
    launchFray(test, scheduler, randomnessProvider, 1, 10000, true) {
      val recording = Path("${path}/recording.json")
      if (recording.exists()) {
        val verifier = ScheduleVerifier(recording.absolutePathString())
        it.scheduleObservers.add(verifier)
      }
    }
  }
}
