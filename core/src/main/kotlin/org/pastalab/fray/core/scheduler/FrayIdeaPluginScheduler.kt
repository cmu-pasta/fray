package org.pastalab.fray.core.scheduler

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.rmi.RemoteScheduler
import java.rmi.registry.LocateRegistry

class FrayIdeaPluginScheduler: Scheduler {
  val registry = LocateRegistry.getRegistry();
  val remoteScheduler = registry.lookup("RemoteScheduler") as RemoteScheduler;

  override fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext {
    val index = remoteScheduler.scheduleNextOperation(threads.map { it.thread.threadId() })
    return threads[index]
  }

  override fun nextIteration(): Scheduler {
    return this
  }
}
