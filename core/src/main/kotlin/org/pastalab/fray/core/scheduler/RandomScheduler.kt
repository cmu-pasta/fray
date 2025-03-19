package org.anonlab.fray.core.scheduler

import kotlinx.serialization.Serializable
import org.anonlab.fray.core.ThreadContext
import org.anonlab.fray.core.randomness.ControlledRandom

@Serializable
class RandomScheduler(val rand: ControlledRandom) : Scheduler {
  constructor() : this(ControlledRandom())

  override fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: List<ThreadContext>
  ): ThreadContext {

    if (threads.size == 1) {
      return threads[0]
    }
    val index = rand.nextInt() % threads.size
    return threads[index]
  }

  override fun nextIteration(): Scheduler {
    return RandomScheduler(ControlledRandom())
  }
}
