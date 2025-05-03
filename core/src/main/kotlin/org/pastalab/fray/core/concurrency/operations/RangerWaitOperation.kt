package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType
import org.pastalab.fray.rmi.ThreadState
import org.pastalab.fray.runtime.RangerCondition

class RangerWaitOperation(val condition: RangerCondition, val threadContext: ThreadContext) :
    BlockedOperation(false, ResourceInfo(condition.id, ResourceType.RANGER_CONDITION)) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    threadContext.pendingOperation = ThreadResumeOperation(true)
    threadContext.state = ThreadState.Runnable
    return null
  }
}
