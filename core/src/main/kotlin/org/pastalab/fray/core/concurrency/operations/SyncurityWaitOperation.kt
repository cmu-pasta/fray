package org.anonlab.fray.core.concurrency.operations

import org.anonlab.fray.core.ThreadContext
import org.anonlab.fray.rmi.ResourceInfo
import org.anonlab.fray.rmi.ResourceType
import org.anonlab.fray.rmi.ThreadState
import org.anonlab.fray.runtime.SyncurityCondition

class SyncurityWaitOperation(val condition: SyncurityCondition, val threadContext: ThreadContext) :
    BlockedOperation(false, ResourceInfo(condition.id, ResourceType.SYNCURITY_CONDITION)) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    threadContext.pendingOperation = ThreadResumeOperation(true)
    threadContext.state = ThreadState.Runnable
    return null
  }
}
