package org.pastalab.fray.core

import java.util.*
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.scheduler.RandomScheduler
import org.pastalab.fray.runtime.Runtime

class TestRunner(val config: Configuration) {

  val context = RunContext(config)
  var currentDivision = 1

  init {
    context.bootstrap()
  }

  fun reportProgress(iteration: Int, bugsFound: Int) {
    if (config.isReplay) return
    if (iteration % currentDivision == 0) {
      print("\u001B[2J")
      print("\u001B[2H")
      println("Fray Testing:")
      println("Report is available at: ${config.report}")
      println("Iterations: $iteration")
      if (bugsFound > 0) {
        println("Bugs Found: $bugsFound")
      }
    }
    if (iteration / currentDivision == 10) {
      currentDivision *= 10
    }
  }

  fun run(): Throwable? {
    config.executionInfo.executor.beforeExecution()
    if (config.noFray) {
      config.executionInfo.executor.execute()
    } else {
      config.frayLogger.info("Fray started.")
      var bugsFound = 0
      if (config.dummyRun) {
        // We want to do a dummy-run first to make sure all variables are initialized
        val noExitWhenBugFound = config.noExitWhenBugFound
        val scheduler = config.scheduler
        val randomnessProvider = config.randomnessProvider
        val observers = config.scheduleObservers
        Runtime.DELEGATE = RuntimeDelegate(context)
        config.noExitWhenBugFound = true
        config.scheduler = RandomScheduler(ControlledRandom())
        config.randomnessProvider = ControlledRandom(mutableListOf(), mutableListOf(), Random(0))
        config.scheduleObservers = mutableListOf()
        Runtime.start()
        try {
          config.executionInfo.executor.beforeExecution()
          config.executionInfo.executor.execute()
          Runtime.onMainExit()
        } catch (e: Throwable) {
          Runtime.onMainExit()
        }
        config.executionInfo.executor.afterExecution()
        config.noExitWhenBugFound = noExitWhenBugFound
        config.scheduler = scheduler
        config.randomnessProvider = randomnessProvider
        config.scheduleObservers = observers
      }
      while (config.shouldRun()) {
        reportProgress(config.currentIteration, bugsFound)
        try {
          if (config.currentIteration != 0) {
            config.scheduler = config.scheduler.nextIteration()
            config.randomnessProvider = ControlledRandom()
          }
          Runtime.DELEGATE = RuntimeDelegate(context)
          Runtime.start()
          config.executionInfo.executor.execute()
          Runtime.onMainExit()
        } catch (e: Throwable) {
          Runtime.onReportError(e)
          Runtime.onMainExit()
        }
        if (config.isReplay || ((context.bugFound != null && context.bugFound !is FrayInternalError)
              && !config.exploreMode)) break
        config.currentIteration++
      }
      context.shutDown()
      config.frayLogger.info(
          "Run finished. Total iter: ${config.currentIteration}, Elapsed time: ${config.elapsedTime()}ms",
      )
    }
    config.executionInfo.executor.afterExecution()
    return context.bugFound
  }
}
