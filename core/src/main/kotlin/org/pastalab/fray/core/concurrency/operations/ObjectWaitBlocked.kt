package org.anonlab.fray.core.concurrency.operations

import org.anonlab.fray.core.concurrency.primitives.ObjectNotifyContext
import org.anonlab.fray.rmi.ResourceInfo
import org.anonlab.fray.rmi.ResourceType

class ObjectWaitBlocked(val objectContext: ObjectNotifyContext, timed: Boolean) :
    BlockedOperation(
        timed,
        ResourceInfo(
            System.identityHashCode(objectContext.lockContext.lockReference.get()),
            ResourceType.CONDITION)) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (objectContext.unblockThread(tid, type)) {
      return objectContext.getSyncObject()
    }
    return null
  }
}
