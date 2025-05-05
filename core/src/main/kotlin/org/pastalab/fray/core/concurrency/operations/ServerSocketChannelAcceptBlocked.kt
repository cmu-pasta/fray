package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.context.ServerSocketChannelContext
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType

class ServerSocketChannelAcceptBlocked(val serverSocketChannelContext: ServerSocketChannelContext) :
    BlockedOperation(false, ResourceInfo(serverSocketChannelContext.port, ResourceType.NETWORK)) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    return null
  }
}
