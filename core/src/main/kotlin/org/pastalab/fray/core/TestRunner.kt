package org.pastalab.fray.core

import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.io.path.div
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.delegates.DelegateSynchronizer
import org.pastalab.fray.core.delegates.RuntimeDelegate
import org.pastalab.fray.runtime.Runtime

class TestRunner(val config: Configuration) {

  val context = RunContext(config)
  var currentDivision = 1
  val stdout = System.out

  fun reportProgress(iteration: Int, bugsFound: Int) {
    if (config.isReplay) return
    if (iteration % currentDivision == 0) {
      stdout.print("\u001B[2J")
      stdout.print("\u001B[2H")
      stdout.println("Fray Testing:")
      stdout.println("Report is available at: ${config.report}")
      stdout.println("Iterations: $iteration")
      if (bugsFound > 0) {
        stdout.println("Bugs Found: $bugsFound")
      }
    }
    if (iteration / currentDivision == 10) {
      currentDivision *= 10
    }
  }

  fun run(): Throwable? {
    config.executionInfo.executor.beforeExecution()
    config.frayLogger.info("Fray started.")
    val bugsFound = 0

    while (config.shouldRun()) {
      if (config.redirectStdout) {
        val stdout = config.report / "stdout.txt"
        System.setOut(PrintStream(FileOutputStream(stdout.toFile())))
        val stderr = config.report / "stderr.txt"
        System.setErr(PrintStream(FileOutputStream(stderr.toFile())))
      }
      reportProgress(config.currentIteration, bugsFound)
      if (config.noFray) {
        try {
          config.executionInfo.executor.execute(config.resetClassLoader)
        } catch (e: Throwable) {}
      } else {
        val t =
            Thread({
              val out = System.out
              val err = System.err
              try {
                val synchronizer = DelegateSynchronizer(context)
                Runtime.NETWORK_DELEGATE = config.networkDelegateType.produce(context, synchronizer)
                Runtime.LOCK_DELEGATE = RuntimeDelegate(context, synchronizer)
                Runtime.start()
                config.executionInfo.executor.execute(config.resetClassLoader)
                Runtime.onMainExit()
              } catch (e: Throwable) {
                Runtime.onReportError(e)
                Runtime.onMainExit()
              }
              System.setOut(out)
              System.setErr(err)
            })
        t.start()
        t.join()
      }
      if (
          config.isReplay ||
              ((context.bugFound != null && context.bugFound !is FrayInternalError) &&
                  !config.exploreMode)
      )
          break
      config.nextIteration()
    }

    context.shutDown()
    config.frayLogger.info(
        "Run finished. Total iter: ${config.currentIteration}, Elapsed time: ${config.elapsedTime()}ms",
        true,
    )
    config.executionInfo.executor.afterExecution()
    return context.bugFound
  }
}
