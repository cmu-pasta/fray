package org.anonlab.fray.core.concurrency.operations

import org.anonlab.fray.core.concurrency.primitives.Acquirable
import org.anonlab.fray.core.concurrency.primitives.InterruptibleContext

class LockBlocked<T>(timed: Boolean, val interruptibleContext: T) :
    BlockedOperation(timed, interruptibleContext.resourceInfo) where
T : Acquirable,
T : InterruptibleContext {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (interruptibleContext.unblockThread(tid, type)) {
      return this
    }
    return null
  }
}
