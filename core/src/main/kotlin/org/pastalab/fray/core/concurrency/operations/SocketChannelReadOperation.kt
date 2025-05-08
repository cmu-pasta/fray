package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.context.SocketChannelContext
import org.pastalab.fray.runtime.MemoryOpType

class SocketChannelReadOperation(val context: SocketChannelContext) :
    RacingOperation(0, MemoryOpType.MEMORY_WRITE) {
  override fun isRacing(op: Operation): Boolean {
    if (op is SocketChannelReadOperation) {
      return true
    }
    return false
  }
}
