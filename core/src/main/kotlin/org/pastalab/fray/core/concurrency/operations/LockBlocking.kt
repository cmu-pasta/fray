package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.primitives.InterruptibleContext
import org.pastalab.fray.core.concurrency.primitives.InterruptionType

class LockBlocking(timed: Boolean, val interruptibleContext: InterruptibleContext) :
    TimedBlockingOperation(timed) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (interruptibleContext.unblockThread(tid, type)) {
      return this
    }
    return null
  }
}
