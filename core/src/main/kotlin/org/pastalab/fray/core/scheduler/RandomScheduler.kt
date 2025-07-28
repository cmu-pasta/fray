package org.pastalab.fray.core.scheduler

import kotlinx.serialization.Serializable
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.randomness.Randomness

@Serializable
class RandomScheduler(val rand: Randomness) : Scheduler {
  constructor() : this(ControlledRandom())

  override fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: Collection<ThreadContext>
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
