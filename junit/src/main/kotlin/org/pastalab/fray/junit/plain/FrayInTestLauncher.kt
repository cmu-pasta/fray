package org.pastalab.fray.junit.plain

import java.io.File
import kotlin.io.path.absolutePathString
import kotlinx.serialization.json.Json
import org.pastalab.fray.core.TestRunner
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.command.ExecutionInfo
import org.pastalab.fray.core.command.LambdaExecutor
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.scheduler.PCTScheduler
import org.pastalab.fray.core.scheduler.Scheduler
import org.pastalab.fray.junit.Common.WORK_DIR

object FrayInTestLauncher {

  fun launchFray(runnable: Runnable, scheduler: Scheduler, randomnessProvider: ControlledRandom) {
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
            10000,
            60,
            scheduler,
            randomnessProvider,
            true,
            false,
            true,
            false,
            false,
            false,
        )
    val runner = TestRunner(config)
    runner.run()?.let { throw it }
  }

  fun launchFrayTest(test: Runnable) {
    launchFray(test, PCTScheduler(ControlledRandom(), 15, 0), ControlledRandom())
  }

  fun launchFrayReplay(test: Runnable, path: String) {
    val randomPath = "${path}/random.json"
    val schedulerPath = "${path}/schedule.json"
    val randomnessProvider = Json.decodeFromString<ControlledRandom>(File(randomPath).readText())
    val scheduler = Json.decodeFromString<Scheduler>(File(schedulerPath).readText())
    launchFray(test, scheduler, randomnessProvider)
  }
}
