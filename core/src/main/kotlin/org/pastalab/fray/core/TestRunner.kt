package org.pastalab.fray.core

import kotlin.time.TimeSource
import org.apache.logging.log4j.Logger
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.scheduler.FifoScheduler
import org.pastalab.fray.core.scheduler.RandomScheduler
import org.pastalab.fray.runtime.Runtime
import java.util.*

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
      while (i != config.iter) {
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
          logger.error("Error found, the recording is saved to ${config.report}/recording_$i/")
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
