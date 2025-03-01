package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.primitives.ObjectNotifyContext
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType

class ObjectWaitBlocked(val objectContext: ObjectNotifyContext, timed: Boolean) :
    BlockedOperation(
        timed,
        ResourceInfo(
            System.identityHashCode(objectContext.objReference.get()), ResourceType.CONDITION)) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (objectContext.unblockThread(tid, type)) {
      return objectContext.getSyncObject()
    }
    return null
  }
}
