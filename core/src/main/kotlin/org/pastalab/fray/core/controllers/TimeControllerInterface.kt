package org.pastalab.fray.core.controllers

import org.pastalab.fray.core.RunContext
import java.time.Instant

abstract class TimeControllerInterface(val context: RunContext) {
  abstract fun currentTimeMillis(): Long
  abstract fun instantNow(): Instant
  abstract fun nanoTime(): Long
  abstract fun addDeadline(deadline: Long)
  abstract fun fastForwardBlockingTime(time: Long)
  abstract fun done()
}
