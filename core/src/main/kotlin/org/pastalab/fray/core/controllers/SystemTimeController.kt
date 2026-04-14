package org.pastalab.fray.core.controllers

import org.pastalab.fray.core.RunContext
import java.time.Instant

class SystemTimeController(context: RunContext): TimeControllerInterface(context) {

  override fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
  }

  override fun instantNow(): Instant {
    return Instant.now()
  }

  override fun nanoTime(): Long {
    return System.nanoTime()
  }

  override fun addDeadline(deadline: Long) {
  }

  override fun fastForwardBlockingTime(time: Long) {
    // We cannot fastforward system time. Just sleep
    Thread.sleep(time)
  }

  override fun done() {
  }
}
