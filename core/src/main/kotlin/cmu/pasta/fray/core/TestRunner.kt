package cmu.pasta.fray.core

import cmu.pasta.fray.core.command.Configuration
import cmu.pasta.fray.core.logger.ConsoleLogger
import cmu.pasta.fray.runtime.Runtime
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.system.measureTimeMillis

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
      val time = measureTimeMillis {
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
          if (GlobalContext.bugFound) {
            println("Error found at iter: $i")
            break
          }
          i++
        }
        GlobalContext.shutDown()
      }
      println("Analysis done in: $time ms")
    }
    config.executionInfo.executor.afterExecution()
    return if (GlobalContext.bugFound) {
      -1
    } else {
      0
    }
  }
}
