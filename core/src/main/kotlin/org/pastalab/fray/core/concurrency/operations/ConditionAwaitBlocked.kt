package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.context.ConditionSignalContext
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType

class ConditionAwaitBlocked(
    val conditionContext: ConditionSignalContext,
    val canInterrupt: Boolean,
    blockedUntil: Long
) :
    BlockedOperation(
        ResourceInfo(
            System.identityHashCode(conditionContext.conditionReference.get()),
            ResourceType.CONDITION),
        blockedUntil) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (conditionContext.unblockThread(tid, type)) {
      return conditionContext.getSyncObject()
    }
    return null
  }
}
