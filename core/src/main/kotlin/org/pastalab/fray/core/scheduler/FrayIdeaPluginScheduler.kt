package org.pastalab.fray.core.scheduler

import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.rmi.RemoteScheduler

class FrayIdeaPluginScheduler : Scheduler {
  val registry = LocateRegistry.getRegistry("localhost", Registry.REGISTRY_PORT)

  val remoteScheduler = registry.lookup("RemoteScheduler") as RemoteScheduler

  override fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: List<ThreadContext>
  ): ThreadContext {
    if (threads.size == 1) {
      return threads.first()
    }
    val index = remoteScheduler.scheduleNextOperation(allThreads.map { it.toStackInfo() })
    return allThreads[index]
  }

  override fun nextIteration(): Scheduler {
    return this
  }
}
