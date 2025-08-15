package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType

class ReactiveBlockingBlocked(val threadContext: ThreadContext) :
    BlockedOperation(
        ResourceInfo(System.identityHashCode(threadContext), ResourceType.NETWORK),
        BLOCKED_OPERATION_NOT_TIMED) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any {
    threadContext.interruptSignaled = true
    threadContext.thread.interrupt()
    return threadContext
  }
}
