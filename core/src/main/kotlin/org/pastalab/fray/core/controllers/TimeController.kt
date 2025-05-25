package org.pastalab.fray.core.controllers

import java.time.Instant
import java.util.concurrent.TimeUnit
import org.pastalab.fray.core.RunContext
import org.pastalab.fray.core.utils.Utils.verifyNoThrow

class TimeController(val context: RunContext) : RunFinishedHandler(context) {
  var nanoTime = TimeUnit.SECONDS.toNanos(1577768400)

  private fun getAndIncrementNanoTime(): Long {
    val currentNanoTime = nanoTime
    nanoTime += TimeUnit.MILLISECONDS.toNanos(10000)
    return currentNanoTime
  }

  fun nanoTime() = verifyNoThrow { getAndIncrementNanoTime() }

  fun currentTimeMillis() = verifyNoThrow { getAndIncrementNanoTime() / 1000000 }

  fun instantNow() = verifyNoThrow { Instant.ofEpochMilli(getAndIncrementNanoTime() / 1000000) }

  override fun done() {
    nanoTime = TimeUnit.SECONDS.toNanos(1577768400)
  }
}
