package org.pastalab.fray.core.scheduler

import org.pastalab.fray.core.ThreadContext

class FifoScheduler : Scheduler {
  override fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext {
    return threads.first()
  }

  override fun done() {}
}
