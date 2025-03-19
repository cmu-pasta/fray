package org.anonlab.fray.core.concurrency.operations

import org.anonlab.fray.core.concurrency.primitives.ConditionSignalContext
import org.anonlab.fray.rmi.ResourceInfo
import org.anonlab.fray.rmi.ResourceType

class ConditionWakeBlocked(val conditionContext: ConditionSignalContext, val noTimeout: Boolean) :
    BlockedOperation(
        false,
        ResourceInfo(
            System.identityHashCode(conditionContext.conditionReference.get()),
            ResourceType.CONDITION)) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (type == InterruptionType.INTERRUPT) {
      return conditionContext.getSyncObject()
    }
    return null
  }
}
