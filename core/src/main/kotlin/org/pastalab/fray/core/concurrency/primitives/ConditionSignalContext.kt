package org.pastalab.fray.core.concurrency.primitives

import java.lang.ref.WeakReference
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.ConditionAwaitBlocked
import org.pastalab.fray.core.concurrency.operations.ConditionWakeBlocked
import org.pastalab.fray.rmi.ThreadState

class ConditionSignalContext(lockContext: LockContext, lock: Lock, val condition: Condition) :
    SignalContext(lockContext) {
  val lockReference = WeakReference(lock)

  override fun sendSignalToObject() {
    lockReference.get()?.lock()
    try {
      condition.signalAll()
    } finally {
      lockReference.get()?.unlock()
    }
  }

  override fun updatedThreadContextDueToUnblock(
      threadContext: ThreadContext,
      type: InterruptionType
  ) {
    threadContext.pendingOperation = ConditionWakeBlocked(this, type != InterruptionType.TIMEOUT)
  }

  override fun updateThreadContextDueToBlock(
      threadContext: ThreadContext,
      timed: Boolean,
      canInterrupt: Boolean
  ) {
    threadContext.pendingOperation = ConditionAwaitBlocked(this, canInterrupt, timed)
    threadContext.state = ThreadState.Paused
  }

  override fun getSyncObject(): Any {
    return lockContext
  }
}
