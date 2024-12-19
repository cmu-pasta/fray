package org.pastalab.fray.core.concurrency.primitives

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.ThreadState
import org.pastalab.fray.core.concurrency.operations.ObjectWaitBlock
import org.pastalab.fray.core.concurrency.operations.ObjectWakeBlocked

class ObjectNotifyContext(lockContext: LockContext, val obj: Any) : SignalContext(lockContext) {
  override fun sendSignalToObject() {
    synchronized(obj) { (obj as Object).notifyAll() }
  }

  override fun updatedThreadContextDueToUnblock(
      threadContext: ThreadContext,
      type: InterruptionType
  ) {
    threadContext.pendingOperation = ObjectWakeBlocked(this, type != InterruptionType.TIMEOUT)
  }

  override fun updateThreadContextDueToBlock(
      threadContext: ThreadContext,
      timedOperation: Boolean,
      canInterrupt: Boolean
  ) {
    threadContext.pendingOperation = ObjectWaitBlock(this, timedOperation)
    threadContext.state = ThreadState.Paused
  }

  override fun getSyncObject(): Any {
    return lockContext
  }
}
