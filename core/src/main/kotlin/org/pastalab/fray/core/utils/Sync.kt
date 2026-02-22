package org.pastalab.fray.core.utils

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.LockSupport

// Simple sync structure to block a thread and wait
// for signals.
class Sync(val goal: Int) : Any() {
  private val count = AtomicInteger(0)
  @Volatile private var blockedThread: Thread? = null
  @Volatile private var isBlockedFlag = false

  fun isBlocked() = isBlockedFlag

  fun blockCheck() {
    Utils.verifyOrReport(count.get() != goal)
    block()
  }

  fun block() {
    if (count.get() >= goal) {
      count.set(0)
      return
    }
    isBlockedFlag = true
    blockedThread = Thread.currentThread()
    while (count.get() < goal) {
      LockSupport.park()
    }
    blockedThread = null
    isBlockedFlag = false
    count.set(0)
  }

  fun unblock() {
    val newCount = count.incrementAndGet()
    Utils.verifyOrReport(newCount <= goal)
    if (newCount >= goal) {
      val t = blockedThread
      if (t != null) {
        LockSupport.unpark(t)
      }
    }
  }
}
