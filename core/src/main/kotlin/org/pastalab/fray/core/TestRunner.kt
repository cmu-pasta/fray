package org.pastalab.fray.core

import kotlin.time.TimeSource
import org.apache.logging.log4j.Logger
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.randomness.ControlledRandom

class TestRunner(val config: Configuration) {

  val context = RunContext(config)

  val logger: Logger = config.loggerContext.getLogger(TestRunner::class.java)

  init {
    context.bootstrap()
  }

  fun reportProgress(iteration: Int, bugsFound: Int) {
    print("\u001B[2J")
    print("\u001B[2H")
    println("Fray Testing:")
    println("Iterations: $iteration")
    println("Bugs Found: $bugsFound")
  }

  fun run(): Throwable? {
    config.executionInfo.executor.beforeExecution()
    if (config.noFray) {
      config.executionInfo.executor.execute()
    } else {
      val timeSource = TimeSource.Monotonic
      val start = timeSource.markNow()
      var i = 0
      var bugsFound = 0
      while (i != config.iter) {
        reportProgress(i, bugsFound)
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
          bugsFound += 1
          println(
              "Error found at iter: $i, Elapsed time: ${(timeSource.markNow() - start).inWholeMilliseconds}ms",
          )
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
