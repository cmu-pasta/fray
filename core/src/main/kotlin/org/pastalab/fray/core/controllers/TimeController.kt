package org.pastalab.fray.core.controllers

import java.time.Instant
import java.util.concurrent.TimeUnit
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.command.SystemTimeDelegateType
import org.pastalab.fray.core.utils.Utils.verifyNoThrow

class TimeController(config: Configuration) {
  var nanoTime = TimeUnit.SECONDS.toNanos(1577768400)
  val isVirtualTimeMode = config.systemTimeDelegateType == SystemTimeDelegateType.MOCK

  fun done() {
    nanoTime = TimeUnit.SECONDS.toNanos(1577768400)
  }

  private fun getAndIncrementNanoTime(): Long {
    val currentNanoTime = nanoTime
    nanoTime += 100_000
    return currentNanoTime
  }

  fun nanoTime() = verifyNoThrow {
    if (!isVirtualTimeMode) System.nanoTime() else getAndIncrementNanoTime()
  }

  fun currentTimeMillis() = verifyNoThrow { currentTimeMillisRaw() }

  fun currentTimeMillisRaw(): Long {
    return if (!isVirtualTimeMode) System.currentTimeMillis()
    else getAndIncrementNanoTime() / 1000000
  }

  fun currentTimeMillisRawNoIncrement(): Long {
    return if (!isVirtualTimeMode) System.currentTimeMillis()
    else nanoTime / 1000000
  }

  fun instantNow() = verifyNoThrow {
    if (!isVirtualTimeMode) Instant.now()
    else Instant.ofEpochMilli(getAndIncrementNanoTime() / 1000000)
  }
}
