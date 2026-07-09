package org.pastalab.fray.core.scheduler

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.randomness.Randomness

class FifoScheduler(rand: Randomness = ControlledRandom()) : Scheduler(rand) {
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
