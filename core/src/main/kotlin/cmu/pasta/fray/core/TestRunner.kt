package cmu.pasta.fray.core

import cmu.pasta.fray.core.command.Configuration
import cmu.pasta.fray.core.logger.ConsoleLogger
import cmu.pasta.fray.core.scheduler.ReplayScheduler
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
    println("Report is available at: ${path.toAbsolutePath()}")
  }

  fun setup() {
    if (config.scheduler !is ReplayScheduler) {
      prepareReportPath(config.report)
      GlobalContext.registerLogger(config.logger)
    }
    GlobalContext.registerLogger(ConsoleLogger())
    GlobalContext.scheduler = config.scheduler
    GlobalContext.config = config
    GlobalContext.bootstrap()
  }

  fun run(): Throwable? {
    config.executionInfo.executor.beforeExecution()
    if (config.noFray) {
      config.executionInfo.executor.execute()
    } else {
      setup()
      val timeSource = TimeSource.Monotonic
      val start = timeSource.markNow()
      var i = 0
      while (i != config.iter) {
        println("Starting iteration $i")
        try {
          Runtime.DELEGATE = RuntimeDelegate()
          Runtime.start()
          config.executionInfo.executor.execute()
          Runtime.onMainExit()
        } catch (e: Throwable) {
          Runtime.onReportError(e)
          Runtime.onMainExit()
        }
        if (GlobalContext.bugFound != null) {
          println(
              "Error found at iter: $i, Elapsed time: ${(timeSource.markNow() - start).inWholeMilliseconds}ms")
          if (!config.exploreMode) {
            break
          }
        }
        i++
      }
      GlobalContext.shutDown()
    }
    config.executionInfo.executor.afterExecution()
    return GlobalContext.bugFound
  }
}
