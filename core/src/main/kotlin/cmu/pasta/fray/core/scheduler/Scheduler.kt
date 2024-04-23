package cmu.pasta.fray.core.scheduler

import cmu.pasta.fray.core.ThreadContext

interface Scheduler {
  fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext

  fun done()
}
