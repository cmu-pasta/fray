package cmu.pasta.sfuzz.core.scheduler

import cmu.pasta.sfuzz.core.ThreadContext
import cmu.pasta.sfuzz.core.concurrency.operations.NonRacingOperation
import cmu.pasta.sfuzz.core.concurrency.operations.RacingOperation
import java.util.Random

class POSScheduler(val rand: Random): Scheduler {
    val threadPriorityQueue = mutableListOf<ThreadContext>()

    override fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext {
        val nonRacingOps = mutableListOf<ThreadContext>()
        for (thread in threads) {
            if (thread !in threadPriorityQueue) {
                val index = rand.nextInt(0, threadPriorityQueue.size)
                threadPriorityQueue.add(index, thread)
            }
            if (thread.pendingOperation is NonRacingOperation) {
                nonRacingOps.add(thread)
            }
        }
        // Schedule all non-racing OPs first.
        if (nonRacingOps.isNotEmpty()) {
            return nonRacingOps.minBy { threadPriorityQueue.indexOf(it) }
        }
        val next = threads.minBy { threadPriorityQueue.indexOf(it)  }
        threadPriorityQueue.removeIf {
            val pendingOp = it.pendingOperation
            if (pendingOp is RacingOperation && it in threads) {
                pendingOp.isRacing(next.pendingOperation)
            } else {
                false
            }
        }
        return next
    }
}