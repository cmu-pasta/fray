package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.context.SelectorContext
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType

class SelectorSelectBlocked(val selectorContext: SelectorContext) :
    BlockedOperation(
        ResourceInfo(System.identityHashCode(selectorContext), ResourceType.NETWORK),
        BLOCKED_OPERATION_NOT_TIMED,
    ) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    selectorContext.unblockThread(tid, type)
    return null
  }
}
