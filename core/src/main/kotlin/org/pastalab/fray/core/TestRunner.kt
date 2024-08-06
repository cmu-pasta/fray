package org.pastalab.fray.core

import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.time.TimeSource
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.logger.ConsoleLogger
import org.pastalab.fray.core.randomness.ControlledRandom

class TestRunner(val config: Configuration) {

  val context = RunContext(config)

  init {
    if (!config.isReplay) {
      prepareReportPath(config.report)
    }
    context.registerLogger(ConsoleLogger())
    context.bootstrap()
  }

  @OptIn(ExperimentalPathApi::class)
  fun prepareReportPath(reportPath: String) {
    val path = Paths.get(reportPath)
    path.deleteRecursively()
    path.createDirectories()
    println("Report is available at: ${path.toAbsolutePath()}")
  }

  fun run(): Throwable? {
    config.executionInfo.executor.beforeExecution()
    if (config.noFray) {
      config.executionInfo.executor.execute()
    } else {
      val timeSource = TimeSource.Monotonic
      val start = timeSource.markNow()
      var i = 0
      while (i != config.iter) {
        println("Starting iteration $i")
        try {
          if (i != 0) {
            config.scheduler = config.scheduler.nextIteration()
            config.randomnessProvider = ControlledRandom()
          }
          org.pastalab.fray.runtime.Runtime.DELEGATE = RuntimeDelegate(context)
          org.pastalab.fray.runtime.Runtime.start()
          config.executionInfo.executor.execute()
          org.pastalab.fray.runtime.Runtime.onMainExit()
        } catch (e: Throwable) {
          org.pastalab.fray.runtime.Runtime.onReportError(e)
          org.pastalab.fray.runtime.Runtime.onMainExit()
        }
        if (context.bugFound != null) {
          println(
              "Error found at iter: $i, Elapsed time: ${(timeSource.markNow() - start).inWholeMilliseconds}ms")
          if (!config.exploreMode) {
            config.saveToReportFolder(i)
            break
          }
        }
        i++
      }
      context.shutDown()
    }
    config.executionInfo.executor.afterExecution()
    return context.bugFound
  }
}
