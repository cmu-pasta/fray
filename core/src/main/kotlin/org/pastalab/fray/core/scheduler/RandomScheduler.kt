package org.pastalab.fray.core.scheduler

import kotlinx.serialization.Serializable
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.randomness.ControlledRandom

@Serializable
class RandomScheduler(val rand: ControlledRandom) : Scheduler {
  constructor() : this(ControlledRandom())

  override fun scheduleNextOperation(threads: List<ThreadContext>, allThreads: List<ThreadContext>): ThreadContext {

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
