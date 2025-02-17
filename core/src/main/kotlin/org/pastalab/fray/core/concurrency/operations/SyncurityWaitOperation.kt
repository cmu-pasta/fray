package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.primitives.Interruptible
import org.pastalab.fray.core.concurrency.primitives.InterruptionType
import org.pastalab.fray.rmi.ThreadState
import org.pastalab.fray.runtime.SyncurityCondition

class SyncurityWaitOperation(val condition: SyncurityCondition, val threadContext: ThreadContext) :
    NonRacingOperation(), Interruptible {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    threadContext.pendingOperation = ThreadResumeOperation(true)
    threadContext.state = ThreadState.Enabled
    return null
  }
}
