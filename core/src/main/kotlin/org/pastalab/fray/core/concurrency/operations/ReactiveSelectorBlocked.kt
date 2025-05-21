package org.pastalab.fray.core.concurrency.operations

import java.nio.channels.Selector
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType

class ReactiveSelectorBlocked(val selector: Selector) :
    BlockedOperation(false, ResourceInfo(System.identityHashCode(selector), ResourceType.NETWORK)) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    selector.wakeup()
    return null
  }
}
