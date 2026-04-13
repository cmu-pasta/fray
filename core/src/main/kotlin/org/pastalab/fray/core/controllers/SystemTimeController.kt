package org.pastalab.fray.core.controllers

import java.time.Instant

class SystemTimeController: TimeControllerInterface {

  override fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
  }

  override fun instantNow(): Instant {
    return Instant.now()
  }

  override fun nanoTime(): Long {
    return System.nanoTime()
  }

  override fun advanceTime() {
  }

  override fun fastForwardBlockingTime(time: Long) {
    // We cannot fastforward system time. Just sleep
    Thread.sleep(time)
  }

  override fun done() {
  }
}
