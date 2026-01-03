package org.pastalab.fray.core.scheduler

import kotlinx.serialization.Serializable
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.randomness.Randomness

@Serializable
sealed interface Scheduler {
  fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: Collection<ThreadContext>,
  ): ThreadContext

  fun nextIteration(randomness: Randomness): Scheduler
}
