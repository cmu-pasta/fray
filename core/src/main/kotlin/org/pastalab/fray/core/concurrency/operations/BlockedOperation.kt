package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.rmi.ResourceInfo

enum class InterruptionType {
  TIMEOUT,
  INTERRUPT,
  FORCE,
  RESOURCE_AVAILABLE,
}

const val BLOCKED_OPERATION_NOT_TIMED = -1L

abstract class BlockedOperation(val resourceInfo: ResourceInfo, val blockedUntil: Long) :
    NonRacingOperation() {
  /**
   * Unblock a thread that is blocked on this operation. If return not null, the thread will be
   * unblocked and synced through the return value.
   */
  abstract fun unblockThread(tid: Long, type: InterruptionType): Any?

  val isTimed: Boolean
    get() = blockedUntil != BLOCKED_OPERATION_NOT_TIMED
}
