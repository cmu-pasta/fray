package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.logger.ConsoleLogger
import cmu.pasta.sfuzz.core.runtime.AnalysisResult
import cmu.pasta.sfuzz.runtime.Delegate
import cmu.pasta.sfuzz.runtime.Runtime
import java.lang.reflect.InvocationTargetException
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively

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
    for (i in 0 ..< config.iter) {
      println("Starting iteration $i")
      try {
        Runtime.DELEGATE = RuntimeDelegate()
        Runtime.start()
        config.exec()
        Runtime.onMainExit()
      } catch (e: InvocationTargetException) {
        GlobalContext.errorFound = true
        Runtime.onMainExit()
        println(e.cause)
      }
      Runtime.DELEGATE = Delegate()
      GlobalContext.done(AnalysisResult.COMPLETE)
      if (GlobalContext.errorFound) {
        println("error found at iter: $i")
        break
      }
    }
    GlobalContext.shutDown()
    println("Analysis done!")
  }
}
