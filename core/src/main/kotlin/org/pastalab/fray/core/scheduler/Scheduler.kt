package org.pastalab.fray.core.scheduler

import org.pastalab.fray.core.ThreadContext

interface Scheduler {
  fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext

  fun done()
}
