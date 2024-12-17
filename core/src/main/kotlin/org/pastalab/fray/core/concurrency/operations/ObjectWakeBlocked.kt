package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.primitives.Interruptible
import org.pastalab.fray.core.concurrency.primitives.InterruptionType
import org.pastalab.fray.core.concurrency.primitives.ObjectNotifyContext

class ObjectWakeBlocked(val objectContext: ObjectNotifyContext, val noTimeout: Boolean) :
    NonRacingOperation(), Interruptible {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    if (type == InterruptionType.INTERRUPT) {
      return objectContext.getSyncObject()
    }
    return null
  }
}
