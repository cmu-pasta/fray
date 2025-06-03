package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.context.CountDownLatchContext
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType

class CountDownLatchAwaitBlocking(blockedUntil: Long, val latchContext: CountDownLatchContext) :
    BlockedOperation(ResourceInfo(latchContext.latchId, ResourceType.CDL), blockedUntil) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (latchContext.unblockThread(tid, type)) {
      return this
    }
    return null
  }
}
