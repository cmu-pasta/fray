package org.anonlab.fray.core.concurrency.operations

import org.anonlab.fray.core.concurrency.primitives.ObjectNotifyContext
import org.anonlab.fray.rmi.ResourceInfo
import org.anonlab.fray.rmi.ResourceType

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
