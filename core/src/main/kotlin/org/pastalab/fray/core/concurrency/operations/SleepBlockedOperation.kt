package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType
import org.pastalab.fray.rmi.ThreadState

class SleepBlockedOperation(val threadContext: ThreadContext, blockedUntil: Long) :
    BlockedOperation(ResourceInfo(0, ResourceType.SLEEP), blockedUntil) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    threadContext.pendingOperation = ThreadResumeOperation(true)
    threadContext.state = ThreadState.Runnable
    return null
  }
}
