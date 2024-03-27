package cmu.pasta.sfuzz.core.scheduler

import cmu.pasta.sfuzz.core.ThreadContext

class FifoScheduler : Scheduler {
  override fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext {
    return threads.first()
  }

  override fun done() {}
}
