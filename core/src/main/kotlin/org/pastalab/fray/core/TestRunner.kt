package org.pastalab.fray.core

import java.util.*
import kotlin.time.TimeSource
import org.apache.logging.log4j.Logger
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.scheduler.RandomScheduler
import org.pastalab.fray.runtime.Runtime

class TestRunner(val config: Configuration) {

  val context = RunContext(config)
  var currentDivision = 1

  val logger: Logger = config.loggerContext.getLogger(TestRunner::class.java)

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
      val timeSource = TimeSource.Monotonic
      val start = timeSource.markNow()
      var i = 0
      logger.info("Fray started.")
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
      while (((timeSource.markNow() - start).inWholeSeconds < config.timeout) &&
          (i != config.iter)) {
        reportProgress(i, bugsFound)
        try {
          if (i != 0) {
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
        if (context.bugFound != null) {
          bugsFound += 1
          println(
              "Error found at iter: $i, Elapsed time: ${(timeSource.markNow() - start).inWholeMilliseconds}ms",
          )
          if (config.isReplay) {
            break
          }
          logger.info(
              "Error found at iter: $i, Elapsed time: ${(timeSource.markNow() - start).inWholeMilliseconds}ms",
          )
          logger.info("The recording is saved to ${config.report}/recording_$i/")
          if (!config.exploreMode) {
            config.saveToReportFolder(i)
            break
          }
        }
        i++
      }
      context.shutDown()
      logger.info(
          "Run finished. Total iter: $i, Elapsed time: ${(timeSource.markNow() - start).inWholeMilliseconds}ms",
      )
    }
    config.executionInfo.executor.afterExecution()
    return context.bugFound
  }
}
