package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.primitives.ConditionSignalContext
import org.pastalab.fray.core.concurrency.primitives.InterruptionType

class ConditionAwaitBlocked(
    val conditionContext: ConditionSignalContext,
    val canInterrupt: Boolean,
    timed: Boolean
) : TimedBlockingOperation(timed) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (conditionContext.unblockThread(tid, type)) {
      return conditionContext.getSyncObject()
    }
    return null
  }
}
