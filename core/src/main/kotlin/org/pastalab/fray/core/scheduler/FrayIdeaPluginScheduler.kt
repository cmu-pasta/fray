package org.pastalab.fray.core.scheduler

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.debugger.DebuggerRegistry
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.randomness.Randomness

// The debugger drives scheduling remotely; randomness only needs to satisfy the base class, so
// delegate to the wrapped scheduler's stream when present.
class FrayIdeaPluginScheduler(val scheduler: Scheduler?) :
    Scheduler(scheduler?.rand ?: ControlledRandom()) {
  val remoteScheduler by lazy { DebuggerRegistry.getRemoteScheduler() }
  var previousScheduleDecision: ThreadContext? = null

  override fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: Collection<ThreadContext>,
  ): ThreadContext {
    val thread = scheduler?.scheduleNextOperation(threads, allThreads)
    return if (thread == null || thread.index != previousScheduleDecision?.index) {
      previousScheduleDecision =
          if (previousScheduleDecision == null || threads.size > 1 || scheduler != null) {
            allThreads
                .toList()[
                    remoteScheduler.scheduleNextOperation(
                        allThreads.map { it.toThreadInfo() },
                        thread?.toThreadInfo(),
                    )]
          } else {
            threads.first()
          }
      previousScheduleDecision!!
    } else {
      thread
    }
  }

  override fun nextIteration(randomness: Randomness): Scheduler {
    return this
  }
}
