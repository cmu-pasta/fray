package cmu.pasta.fray.core

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

  fun run() {
    setup()
    val time = measureTimeMillis {
      for (i in 0 ..< config.iter) {
        println("Starting iteration $i")
        try {
          Runtime.DELEGATE = RuntimeDelegate()
          Runtime.start()
          config.exec()
          Runtime.onMainExit()
        } catch (e: Throwable) {
          Runtime.onReportError(e)
          Runtime.onMainExit()
        }
        if (GlobalContext.bugFound) {
          println("Error found at iter: $i")
          break
        }
      }
      GlobalContext.shutDown()
    }
    println("Analysis done in: $time ms")
  }
}
