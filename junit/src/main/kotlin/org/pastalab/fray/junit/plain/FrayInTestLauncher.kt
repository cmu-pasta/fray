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
import org.pastalab.fray.core.observers.ScheduleVerifier
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.scheduler.PCTScheduler
import org.pastalab.fray.core.scheduler.Scheduler
import org.pastalab.fray.junit.Common.WORK_DIR

object FrayInTestLauncher {

  fun launchFray(
      runnable: Runnable,
      scheduler: Scheduler,
      randomnessProvider: ControlledRandom,
      iteration: Int,
      timeout: Int,
      additionalConfigs: (Configuration) -> Unit = { _ -> }
  ) {
    val config =
        Configuration(
            ExecutionInfo(
                LambdaExecutor(
                    { runnable.run() },
                ),
                false,
                false,
                -1),
            WORK_DIR.absolutePathString(),
            iteration,
            timeout,
            scheduler,
            randomnessProvider,
            true,
            false,
            true,
            false,
            false,
            false,
        )
    additionalConfigs(config)
    val runner = TestRunner(config)
    runner.run()?.let { throw it }
  }

  fun launchFrayTest(test: Runnable) {
    launchFray(test, PCTScheduler(ControlledRandom(), 15, 0), ControlledRandom(), 10000, 120)
  }

  fun launchFrayReplay(test: Runnable, path: String) {
    val randomPath = "${path}/random.json"
    val schedulerPath = "${path}/schedule.json"
    val randomnessProvider = Json.decodeFromString<ControlledRandom>(File(randomPath).readText())
    val scheduler = Json.decodeFromString<Scheduler>(File(schedulerPath).readText())
    launchFray(test, scheduler, randomnessProvider, 1, 10000) {
      val recording = Path("${path}/recording.json")
      if (recording.exists()) {
        val verifier = ScheduleVerifier(recording.absolutePathString())
        it.scheduleObservers.add(verifier)
      }
    }
  }
}
