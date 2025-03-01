package org.pastalab.fray.core.concurrency.primitives

import java.lang.ref.WeakReference
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.ConditionAwaitBlocked
import org.pastalab.fray.core.concurrency.operations.ConditionWakeBlocked
import org.pastalab.fray.core.concurrency.operations.InterruptionType
import org.pastalab.fray.rmi.ThreadState

class ConditionSignalContext(lockContext: LockContext, lock: Lock, condition: Condition) :
    SignalContext(lockContext) {
  val lockReference = WeakReference(lock)
  val conditionReference = WeakReference(condition)

  override fun sendSignalToObject() {
    lockReference.get()?.lock()
    try {
      conditionReference.get()?.signalAll()
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
    threadContext.state = ThreadState.Blocked
  }

  override fun getSyncObject(): Any {
    return lockContext
  }
}
