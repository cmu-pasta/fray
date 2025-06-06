package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.runtime.MemoryOpType

const val REACTIVE_BLOCKING_RESOURCE_ID = -1

class ReactiveBlockingOperation : RacingOperation(-1, MemoryOpType.MEMORY_WRITE) {
  override fun isRacing(op: Operation): Boolean {
    return op is ReactiveBlockingOperation
  }
}
