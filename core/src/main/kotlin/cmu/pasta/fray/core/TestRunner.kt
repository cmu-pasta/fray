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

  fun createContext(): GlobalContext {
    val context = GlobalContext(config)
    if (config.scheduler !is ReplayScheduler) {
      prepareReportPath(config.report)
      config.loggers.forEach(context::registerLogger)
    }
    context.registerLogger(ConsoleLogger())
    context.bootstrap()
    return context
  }

  fun run(): Throwable? {
    config.executionInfo.executor.beforeExecution()
    val context = createContext()
    if (config.noFray) {
      config.executionInfo.executor.execute()
    } else {
      val timeSource = TimeSource.Monotonic
      val start = timeSource.markNow()
      var i = 0
      while (i != config.iter) {
        println("Starting iteration $i")
        try {
          Runtime.DELEGATE = RuntimeDelegate(context)
          Runtime.start()
          config.executionInfo.executor.execute()
          Runtime.onMainExit()
        } catch (e: Throwable) {
          Runtime.onReportError(e)
          Runtime.onMainExit()
        }
        if (context.bugFound != null) {
          println(
              "Error found at iter: $i, Elapsed time: ${(timeSource.markNow() - start).inWholeMilliseconds}ms")
          if (!config.exploreMode) {
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
