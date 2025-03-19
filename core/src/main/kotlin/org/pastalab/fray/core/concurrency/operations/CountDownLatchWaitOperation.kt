package org.anonlab.fray.core.concurrency.operations

import org.anonlab.fray.core.concurrency.primitives.CountDownLatchContext
import org.anonlab.fray.rmi.ResourceInfo
import org.anonlab.fray.rmi.ResourceType

class CountDownLatchAwaitBlocking(timed: Boolean, val latchContext: CountDownLatchContext) :
    BlockedOperation(timed, ResourceInfo(latchContext.latchId, ResourceType.CDL)) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (latchContext.unblockThread(tid, type)) {
      return this
    }
    return null
  }
}
