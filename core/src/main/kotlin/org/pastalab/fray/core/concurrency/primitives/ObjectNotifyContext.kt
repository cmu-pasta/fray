package org.anonlab.fray.core.concurrency.primitives

import org.anonlab.fray.core.ThreadContext
import org.anonlab.fray.core.concurrency.operations.InterruptionType
import org.anonlab.fray.core.concurrency.operations.ObjectWaitBlocked
import org.anonlab.fray.core.concurrency.operations.ObjectWakeBlocked
import org.anonlab.fray.rmi.ThreadState

class ObjectNotifyContext(lockContext: LockContext, obj: Any) : SignalContext(lockContext) {

  override fun sendSignalToObject() {
    val obj = lockContext.lockReference.get() ?: return
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
    threadContext.pendingOperation = ObjectWaitBlocked(this, timedOperation)
    threadContext.state = ThreadState.Blocked
  }

  override fun getSyncObject(): Any {
    return lockContext
  }
}
