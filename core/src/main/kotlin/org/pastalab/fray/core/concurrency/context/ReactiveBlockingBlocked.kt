package org.pastalab.fray.core.concurrency.context

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.BlockedOperation
import org.pastalab.fray.core.concurrency.operations.InterruptionType
import org.pastalab.fray.rmi.ResourceInfo

class ReactiveBlockingBlocked(val threadContext: ThreadContext) :
    BlockedOperation(
        false,
        ResourceInfo(
            System.identityHashCode(threadContext), org.pastalab.fray.rmi.ResourceType.NETWORK)) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    threadContext.interruptSignaled = true
    threadContext.thread.interrupt()
    return threadContext
  }
}
