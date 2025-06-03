package org.pastalab.fray.core.concurrency.context

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.InterruptionType
import org.pastalab.fray.core.concurrency.operations.ObjectWaitBlocked
import org.pastalab.fray.core.concurrency.operations.ObjectWakeBlocked
import org.pastalab.fray.rmi.ThreadState

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
      blockedUntil: Long,
      canInterrupt: Boolean
  ) {
    threadContext.pendingOperation = ObjectWaitBlocked(this, blockedUntil)
    threadContext.state = ThreadState.Blocked
  }

  override fun getSyncObject(): Any {
    return lockContext
  }
}
