package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.context.SelectorContext
import org.pastalab.fray.runtime.MemoryOpType

// FIXME(aoli): We need to figure out how to pass the resource id for network operations.
class SelectorSelectOperation(selectorContext: SelectorContext) :
    RacingOperation(0, MemoryOpType.MEMORY_WRITE) {
  override fun isRacing(op: Operation): Boolean {
    if (op is SelectorSelectOperation) {
      return true
    }
    return false
  }
}
