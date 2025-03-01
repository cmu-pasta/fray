package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType
import org.pastalab.fray.rmi.ThreadState
import org.pastalab.fray.runtime.SyncurityCondition

class SyncurityWaitOperation(val condition: SyncurityCondition, val threadContext: ThreadContext) :
    BlockedOperation(false, ResourceInfo(condition.id, ResourceType.SYNCURITY_CONDITION)) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    threadContext.pendingOperation = ThreadResumeOperation(true)
    threadContext.state = ThreadState.Runnable
    return null
  }
}
