package org.anonlab.fray.core.concurrency.operations

import org.anonlab.fray.rmi.ResourceInfo

enum class InterruptionType {
  TIMEOUT,
  INTERRUPT,
  FORCE,
  RESOURCE_AVAILABLE,
}

abstract class BlockedOperation(val timed: Boolean, val resourceInfo: ResourceInfo) :
    NonRacingOperation() {
  /**
   * Unblock a thread that is blocked on this operation. If return not null, the thread will be
   * unblocked and synced through the return value.
   */
  abstract fun unblockThread(tid: Long, type: InterruptionType): Any?
}
