package org.pastalab.fray.core.concurrency.context

import java.lang.ref.WeakReference
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.ConditionAwaitBlocked
import org.pastalab.fray.core.concurrency.operations.ConditionWakeBlocked
import org.pastalab.fray.core.concurrency.operations.InterruptionType
import org.pastalab.fray.core.utils.Utils.verifyOrReport
import org.pastalab.fray.rmi.ThreadState

class ConditionSignalContext(lockContext: LockContext, condition: Condition) :
    SignalContext(lockContext) {
  val conditionReference = WeakReference(condition)

  override fun sendSignalToObject() {
    verifyOrReport(lockContext.lockReference.get() is Lock) {
      "ConditionSignalContext should only be used with Lock objects"
    }
    val lock = lockContext.lockReference.get() as Lock? ?: return
    lock.lock()
    try {
      conditionReference.get()?.signalAll()
    } finally {
      lock.unlock()
    }
  }

  override fun updatedThreadContextDueToUnblock(
      threadContext: ThreadContext,
      type: InterruptionType,
  ) {
    threadContext.pendingOperation = ConditionWakeBlocked(this, type != InterruptionType.TIMEOUT)
  }

  override fun updateThreadContextDueToBlock(
      threadContext: ThreadContext,
      blockedUntil: Long,
      canInterrupt: Boolean,
  ) {
    threadContext.pendingOperation = ConditionAwaitBlocked(this, canInterrupt, blockedUntil)
    threadContext.state = ThreadState.Blocked
  }

  override fun getSyncObject(): Any {
    return lockContext
  }
}
