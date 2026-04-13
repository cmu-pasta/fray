package org.pastalab.fray.core.controllers

import java.time.Instant

interface TimeControllerInterface {
  fun currentTimeMillis(): Long
  fun instantNow(): Instant
  fun nanoTime(): Long
  fun advanceTime()
  fun fastForwardBlockingTime(time: Long)
  fun done()
}
