package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.context.ServerSocketChannelContext
import org.pastalab.fray.runtime.MemoryOpType

class ServerSocketChannelAcceptOperation(val context: ServerSocketChannelContext) :
    RacingOperation(0, MemoryOpType.MEMORY_WRITE) {
  override fun isRacing(op: Operation): Boolean {
    if (op is ServerSocketChannelAcceptOperation) {
      return true
    }
    return false
  }
}
