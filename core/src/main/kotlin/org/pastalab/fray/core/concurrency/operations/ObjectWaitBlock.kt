package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.primitives.InterruptionType
import org.pastalab.fray.core.concurrency.primitives.ObjectNotifyContext

class ObjectWaitBlock(val objectContext: ObjectNotifyContext, timed: Boolean) :
    TimedBlockingOperation(timed) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (objectContext.unblockThread(tid, type)) {
      return objectContext.getSyncObject()
    }
    return null
  }
}
