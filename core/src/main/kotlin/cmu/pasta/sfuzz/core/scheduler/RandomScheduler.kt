package cmu.pasta.sfuzz.core.scheduler

import cmu.pasta.sfuzz.core.ThreadContext

class RandomScheduler : Scheduler {
  override fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext {
    return threads.random()
  }
}
