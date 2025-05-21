package org.pastalab.fray.core

import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.controllers.ProactiveNetworkController
import org.pastalab.fray.core.controllers.TimeController
import org.pastalab.fray.core.delegates.DelegateSynchronizer
import org.pastalab.fray.core.delegates.ProactiveNetworkDelegate
import org.pastalab.fray.core.delegates.RuntimeDelegate
import org.pastalab.fray.core.delegates.TimeDelegate
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.runtime.Runtime

class TestRunner(val config: Configuration) {

  val context = RunContext(config)
  var currentDivision = 1

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
    config.frayLogger.info("Fray started.")
    var bugsFound = 0
    while (config.shouldRun()) {
      reportProgress(config.currentIteration, bugsFound)
      if (config.noFray) {
        try {
          config.executionInfo.executor.execute()
        } catch (e: Throwable) {}
      } else {
        try {
          if (config.currentIteration != 0) {
            config.scheduler = config.scheduler.nextIteration()
            config.randomnessProvider = ControlledRandom()
          }
          val synchronizer = DelegateSynchronizer(context)
          Runtime.NETWORK_DELEGATE =
              ProactiveNetworkDelegate(ProactiveNetworkController(context), synchronizer)
          Runtime.TIME_DELEGATE = TimeDelegate(TimeController(context), synchronizer)
          Runtime.LOCK_DELEGATE = RuntimeDelegate(context, synchronizer)
          Runtime.start()
          config.executionInfo.executor.execute()
          Runtime.onMainExit()
        } catch (e: Throwable) {
          Runtime.onReportError(e)
          Runtime.onMainExit()
        }
      }
      if (config.isReplay ||
          ((context.bugFound != null && context.bugFound !is FrayInternalError) &&
              !config.exploreMode))
          break
      config.currentIteration++
    }
    context.shutDown()
    config.frayLogger.info(
        "Run finished. Total iter: ${config.currentIteration}, Elapsed time: ${config.elapsedTime()}ms",
        true)
    config.executionInfo.executor.afterExecution()
    return context.bugFound
  }
}
