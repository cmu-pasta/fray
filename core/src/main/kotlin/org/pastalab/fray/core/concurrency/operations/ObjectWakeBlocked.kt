package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.context.ObjectNotifyContext
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType

class ObjectWakeBlocked(val objectContext: ObjectNotifyContext, val noTimeout: Boolean) :
    BlockedOperation(
        false,
        ResourceInfo(
            System.identityHashCode(objectContext.lockContext.lockReference.get()),
            ResourceType.CONDITION)) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (type == InterruptionType.INTERRUPT) {
      return objectContext.getSyncObject()
    }
    return null
  }
}
