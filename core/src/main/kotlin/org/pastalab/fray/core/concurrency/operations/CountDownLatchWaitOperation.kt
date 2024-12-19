package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.primitives.CountDownLatchContext
import org.pastalab.fray.core.concurrency.primitives.InterruptionType

class CountDownLatchAwaitBlocking(timed: Boolean, val latchContext: CountDownLatchContext) :
    TimedBlockingOperation(timed) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (latchContext.unblockThread(tid, type)) {
      return this
    }
    return null
  }
}
