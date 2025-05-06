package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.context.ServerSocketChannelContext
import org.pastalab.fray.core.concurrency.context.SocketChannelContext
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType

class SocketChannelReadBlocked(val socketChannelContext: SocketChannelContext) :
    BlockedOperation(false, ResourceInfo(0, ResourceType.NETWORK)) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    return null
  }
}
