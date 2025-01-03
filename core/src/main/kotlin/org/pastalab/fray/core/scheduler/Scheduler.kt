package org.pastalab.fray.core.scheduler

import kotlinx.serialization.Serializable
import org.pastalab.fray.core.ThreadContext

@Serializable
sealed interface Scheduler {
  fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: List<ThreadContext>
  ): ThreadContext

  fun nextIteration(): Scheduler
}
