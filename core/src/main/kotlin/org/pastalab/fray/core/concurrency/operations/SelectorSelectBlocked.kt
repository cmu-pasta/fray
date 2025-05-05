package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.context.SelectorContext
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType

class SelectorSelectBlocked(val selectorContext: SelectorContext) :
    BlockedOperation(
        false, ResourceInfo(System.identityHashCode(selectorContext), ResourceType.NETWORK)) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    TODO("Not yet implemented")
  }
}
