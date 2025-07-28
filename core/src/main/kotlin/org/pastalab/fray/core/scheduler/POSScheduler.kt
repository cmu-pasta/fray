package org.pastalab.fray.core.scheduler

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.NonRacingOperation
import org.pastalab.fray.core.concurrency.operations.RacingOperation
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.randomness.Randomness

@Serializable
class POSScheduler(val rand: Randomness) : Scheduler {
  constructor() : this(ControlledRandom()) {}

  @Transient val threadPriority = mutableMapOf<ThreadContext, Double>()
  @Transient val nonRacingOperationBuffer = mutableListOf<ThreadContext>()

  override fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: Collection<ThreadContext>
  ): ThreadContext {
    if (threads.size == 1) {
      return threads[0]
    }
    nonRacingOperationBuffer.clear()
    for (thread in threads) {
      if (thread !in threadPriority) {
        val priority = rand.nextDouble(0.0, 1.0)
        threadPriority[thread] = priority
      }
      if (thread.pendingOperation is NonRacingOperation) {
        nonRacingOperationBuffer.add(thread)
      }
    }
    // Schedule all non-racing OPs first.
    if (nonRacingOperationBuffer.isNotEmpty()) {
      return nonRacingOperationBuffer.minBy { threadPriority[it]!! }
    }
    val next = threads.minBy { threadPriority[it]!! }
    threadPriority.keys.removeIf {
      val pendingOp = it.pendingOperation
      val res =
          if (it == next) {
            true
          } else if (pendingOp is RacingOperation && it in threads) {
            pendingOp.isRacing(next.pendingOperation)
          } else {
            false
          }
      res
    }
    return next
  }

  override fun nextIteration(randomness: Randomness): Scheduler {
    return POSScheduler(randomness)
  }
}
