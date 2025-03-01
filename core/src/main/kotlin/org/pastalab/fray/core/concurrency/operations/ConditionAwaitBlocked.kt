package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.primitives.ConditionSignalContext
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType

class ConditionAwaitBlocked(
    val conditionContext: ConditionSignalContext,
    val canInterrupt: Boolean,
    timed: Boolean
) :
    BlockedOperation(
        timed,
        ResourceInfo(
            System.identityHashCode(conditionContext.conditionReference.get()),
            ResourceType.CONDITION)) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (conditionContext.unblockThread(tid, type)) {
      return conditionContext.getSyncObject()
    }
    return null
  }
}
