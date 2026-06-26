package org.pastalab.fray.core.scheduler

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.randomness.Randomness

class RandomScheduler(rand: Randomness) : Scheduler(rand) {
  constructor() : this(ControlledRandom())

  override fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: Collection<ThreadContext>,
  ): ThreadContext {

    if (threads.size == 1) {
      return threads[0]
    }
    val index = rand.nextInt() % threads.size
    return threads[index]
  }

  override fun nextIteration(randomness: Randomness): Scheduler {
    return RandomScheduler(randomness)
  }
}
