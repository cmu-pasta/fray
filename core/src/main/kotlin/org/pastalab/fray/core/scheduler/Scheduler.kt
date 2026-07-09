package org.pastalab.fray.core.scheduler

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.randomness.Randomness

abstract class Scheduler(val rand: Randomness) {
  abstract fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: Collection<ThreadContext>,
  ): ThreadContext

  abstract fun nextIteration(randomness: Randomness): Scheduler

  /** Random number requested by the runtime (e.g. spurious wakeups, signal target selection). */
  fun nextInt(): Int = rand.nextInt()

  fun nextDouble(): Double = rand.nextDouble()

  fun nextDouble(origin: Double, bound: Double): Double = rand.nextDouble(origin, bound)
}
