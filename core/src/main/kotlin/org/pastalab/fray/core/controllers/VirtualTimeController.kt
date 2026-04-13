package org.pastalab.fray.core.controllers

import java.time.Instant
import java.util.SortedSet
import java.util.TreeSet
import java.util.concurrent.locks.ReentrantLock

class VirtualTimeController: TimeControllerInterface {
  val deadlineSet = TreeSet<Int>()
  var running = false
  @Volatile
  var currentTime: Long = 0

  init {
    while (running) {
      while (deadlineSet.isNotEmpty()) {
        currentTime += deadlineSet.first()
      }
    }
  }

  override fun currentTimeMillis(): Long {
  }

  override fun instantNow(): Instant {
  }

  override fun nanoTime(): Long {
  }

  override fun advanceTime() {
  }

  override fun fastForwardBlockingTime(time: Long) {
  }

  override fun done() {
  }
}
