package cmu.pasta.fray.core.scheduler

import cmu.pasta.fray.core.ThreadContext

class FifoScheduler : Scheduler {
  override fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext {
    return threads.first()
  }

  override fun done() {}
}
