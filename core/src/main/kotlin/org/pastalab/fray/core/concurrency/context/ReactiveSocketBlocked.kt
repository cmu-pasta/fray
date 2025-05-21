package org.pastalab.fray.core.concurrency.context

import java.nio.channels.spi.AbstractInterruptibleChannel
import org.pastalab.fray.core.concurrency.operations.BlockedOperation
import org.pastalab.fray.core.concurrency.operations.InterruptionType
import org.pastalab.fray.rmi.ResourceInfo

class ReactiveSocketBlocked(val channel: AbstractInterruptibleChannel) :
    BlockedOperation(
        false,
        ResourceInfo(
            System.identityHashCode(channel), org.pastalab.fray.rmi.ResourceType.NETWORK)) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    channel.close()
    return null
  }
}
