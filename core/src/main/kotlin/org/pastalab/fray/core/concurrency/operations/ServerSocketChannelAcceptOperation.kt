package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.context.ServerSocketChannelContext
import org.pastalab.fray.runtime.MemoryOpType

class ServerSocketChannelAcceptOperation(val context: ServerSocketChannelContext) :
    RacingOperation(0, MemoryOpType.MEMORY_WRITE) {
  override fun isRacing(op: Operation): Boolean {
    return op is ServerSocketChannelAcceptOperation
  }
}
