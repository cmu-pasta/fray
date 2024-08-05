package org.pastalab.fray.core.scheduler

import java.util.Random
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.NonRacingOperation
import org.pastalab.fray.core.concurrency.operations.RacingOperation

class POSScheduler(val rand: Random) : Scheduler {
  constructor() : this(Random()) {}

  val threadPriority = mutableMapOf<ThreadContext, Double>()

  override fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext {
    if (threads.size == 1) {
      return threads[0]
    }
    val nonRacingOps = mutableListOf<ThreadContext>()
    for (thread in threads) {
      if (thread !in threadPriority) {
        val priority = rand.nextDouble(0.0, 1.0)
        threadPriority[thread] = priority
      }
      if (thread.pendingOperation is NonRacingOperation) {
        nonRacingOps.add(thread)
      }
    }
    // Schedule all non-racing OPs first.
    if (nonRacingOps.isNotEmpty()) {
      return nonRacingOps.minBy { threadPriority[it]!! }
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

  override fun done() {
    threadPriority.clear()
  }
}
