package cmu.pasta.fray.core

import cmu.pasta.fray.core.command.Configuration
import cmu.pasta.fray.core.logger.ConsoleLogger
import cmu.pasta.fray.runtime.Runtime
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.time.TimeSource

class TestRunner(val config: Configuration) {

  @OptIn(ExperimentalPathApi::class)
  fun prepareReportPath(reportPath: String) {
    val path = Paths.get(reportPath)
    path.deleteRecursively()
    path.createDirectories()
  }

  fun setup() {
    prepareReportPath(config.report)
    GlobalContext.registerLogger(config.logger)
    GlobalContext.registerLogger(ConsoleLogger())
    GlobalContext.scheduler = config.scheduler
    GlobalContext.config = config
    GlobalContext.bootstrap()
  }

  fun run(): Int {
    config.executionInfo.executor.beforeExecution()
    if (config.noFray) {
      config.executionInfo.executor.execute()
    } else {
      setup()
      val outputFile = Paths.get(config.report, "output.txt").toFile()
      val timeSource = TimeSource.Monotonic
      val start = timeSource.markNow()
      var i = 0
      while (i != config.iter) {
        outputFile.appendText("Starting iteration $i\n")
        try {
          Runtime.DELEGATE = RuntimeDelegate()
          Runtime.start()
          config.executionInfo.executor.execute()
          Runtime.onMainExit()
        } catch (e: Throwable) {
          Runtime.onReportError(e)
          Runtime.onMainExit()
        }
        if (GlobalContext.bugFound) {
          outputFile.appendText(
              "Error found at iter: $i, Elapsed time: ${(timeSource.markNow() - start).inWholeMilliseconds}ms\n")
          if (!config.exploreMode) {
            break
          }
        }
        i++
      }
      GlobalContext.shutDown()
    }
    config.executionInfo.executor.afterExecution()
    return if (GlobalContext.bugFound) {
      -1
    } else {
      0
    }
  }
}
