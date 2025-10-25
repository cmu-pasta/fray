package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.context.Acquirable
import org.pastalab.fray.core.concurrency.context.InterruptibleContext

class LockBlocked<T>(blockedUntil: Long, val interruptibleContext: T) :
    BlockedOperation(interruptibleContext.resourceInfo, blockedUntil)
    where T : Acquirable, T : InterruptibleContext {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (interruptibleContext.unblockThread(tid, type)) {
      return this
    }
    return null
  }
}
