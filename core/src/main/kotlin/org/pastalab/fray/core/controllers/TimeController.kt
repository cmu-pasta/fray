package org.pastalab.fray.core.controllers

import java.time.Instant
import java.util.concurrent.TimeUnit
import org.pastalab.fray.core.RunContext

class TimeController(val context: RunContext) : RunFinishedHandler(context) {
  var nanoTime = TimeUnit.SECONDS.toNanos(1577768400)

  fun nanoTime(): Long {
    nanoTime += TimeUnit.MILLISECONDS.toNanos(10000)
    return nanoTime
  }

  fun currentTimeMillis(): Long {
    return nanoTime() / 1000000
  }

  fun instantNow(): Instant {
    return Instant.ofEpochMilli(currentTimeMillis())
  }

  override fun done() {
    nanoTime = TimeUnit.SECONDS.toNanos(1577768400)
  }
}
