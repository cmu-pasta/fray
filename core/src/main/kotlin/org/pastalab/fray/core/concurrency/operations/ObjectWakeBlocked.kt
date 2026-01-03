package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.context.ObjectNotifyContext
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType

class ObjectWakeBlocked(val objectContext: ObjectNotifyContext, val noTimeout: Boolean) :
    BlockedOperation(
        ResourceInfo(
            System.identityHashCode(objectContext.lockContext.lockReference.get()),
            ResourceType.CONDITION,
        ),
        BLOCKED_OPERATION_NOT_TIMED,
    ) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (objectContext.lockContext.canLock(tid)) {
      return objectContext.getSyncObject()
    }
    return null
  }
}
