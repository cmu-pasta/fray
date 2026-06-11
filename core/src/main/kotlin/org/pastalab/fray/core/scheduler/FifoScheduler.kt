package org.pastalab.fray.core.scheduler

import kotlinx.serialization.Serializable
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.randomness.Randomness

@Serializable
class FifoScheduler : Scheduler {
  override fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: Collection<ThreadContext>,
  ): ThreadContext {
    return threads.first()
  }

  override fun nextIteration(randomness: Randomness): Scheduler {
    return this
  }
}
