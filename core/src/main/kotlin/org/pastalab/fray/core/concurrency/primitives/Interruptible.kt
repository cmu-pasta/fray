package org.pastalab.fray.core.concurrency.primitives

enum class InterruptionType {
  TIMEOUT,
  INTERRUPT,
  FORCE,
  RESOURCE_AVAILABLE,
}

interface Interruptible {
  /**
   * Unblock a thread that is blocked on this operation. If return not null, the thread will be
   * unblocked and synced through the return value.
   */
  fun unblockThread(tid: Long, type: InterruptionType): Any?
}
