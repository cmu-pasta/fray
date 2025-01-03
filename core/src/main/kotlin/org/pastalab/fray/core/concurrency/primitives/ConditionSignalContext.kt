package org.pastalab.fray.core.concurrency.primitives

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.ConditionAwaitBlocked
import org.pastalab.fray.core.concurrency.operations.ConditionWakeBlocked
import org.pastalab.fray.rmi.ThreadState

class ConditionSignalContext(lockContext: LockContext, val lock: Lock, val condition: Condition) :
    SignalContext(lockContext) {
  override fun sendSignalToObject() {
    lock.lock()
    try {
      condition.signalAll()
    } finally {
      lock.unlock()
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
