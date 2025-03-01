package org.pastalab.fray.core.concurrency.primitives

import java.lang.ref.WeakReference
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.InterruptionType
import org.pastalab.fray.core.concurrency.operations.ObjectWaitBlocked
import org.pastalab.fray.core.concurrency.operations.ObjectWakeBlocked
import org.pastalab.fray.rmi.ThreadState

class ObjectNotifyContext(lockContext: LockContext, obj: Any) : SignalContext(lockContext) {
  val objReference = WeakReference(obj)

  override fun sendSignalToObject() {
    objReference.get()?.let { obj -> synchronized(obj) { (obj as Object).notifyAll() } }
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
