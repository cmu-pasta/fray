package org.pastalab.fray.core.scheduler

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.debugger.DebuggerRegistry

@Serializable
class FrayIdeaPluginScheduler(val scheduler: Scheduler?) : Scheduler {
  @kotlinx.serialization.Transient val remoteScheduler = DebuggerRegistry.getRemoteScheduler()
  @Contextual var previousScheduleDecision: ThreadContext? = null

  override fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: Collection<ThreadContext>
  ): ThreadContext {
    val thread = scheduler?.scheduleNextOperation(threads, allThreads)
    return if (thread == null || thread.index != previousScheduleDecision?.index) {
      previousScheduleDecision =
          if (previousScheduleDecision == null || threads.size > 1 || scheduler != null) {
            allThreads
                .toList()[
                    remoteScheduler.scheduleNextOperation(
                        allThreads.map { it.toThreadInfo() }, thread?.toThreadInfo())]
          } else {
            threads.first()
          }
      previousScheduleDecision!!
    } else {
      thread
    }
  }

  override fun nextIteration(): Scheduler {
    return this
  }
}
