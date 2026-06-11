package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.context.ConditionSignalContext
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType

class ConditionWakeBlocked(val conditionContext: ConditionSignalContext, val noTimeout: Boolean) :
    BlockedOperation(
        ResourceInfo(
            System.identityHashCode(conditionContext.conditionReference.get()),
            ResourceType.CONDITION,
        ),
        BLOCKED_OPERATION_NOT_TIMED,
    ) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (conditionContext.lockContext.canLock(tid)) {
      return conditionContext.getSyncObject()
    }
    return null
  }
}
