package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.primitives.ConditionSignalContext
import org.pastalab.fray.core.concurrency.primitives.Interruptible
import org.pastalab.fray.core.concurrency.primitives.InterruptionType

class ConditionWakeBlocked(val conditionContext: ConditionSignalContext, val noTimeout: Boolean) :
    NonRacingOperation(), Interruptible {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (type == InterruptionType.INTERRUPT) {
      return conditionContext.getSyncObject()
    }
    return null
  }
}
