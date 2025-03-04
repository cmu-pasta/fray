package org.pastalab.fray.core.scheduler

import kotlinx.serialization.Serializable
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.debugger.DebuggerRegistry

@Serializable
class FrayIdeaPluginScheduler : Scheduler {
  @Transient val remoteScheduler = DebuggerRegistry.getRemoteScheduler()

  override fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: List<ThreadContext>
  ): ThreadContext {
    if (threads.size == 1) {
      return threads.first()
    }
    val index = remoteScheduler.scheduleNextOperation(allThreads.map { it.toThreadInfo() })
    return allThreads[index]
  }

  override fun nextIteration(): Scheduler {
    return this
  }
}
