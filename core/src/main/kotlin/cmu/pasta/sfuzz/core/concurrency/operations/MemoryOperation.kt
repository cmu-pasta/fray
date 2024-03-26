package cmu.pasta.sfuzz.core.concurrency.operations

import cmu.pasta.sfuzz.runtime.MemoryOpType

class MemoryOperation(obj: Int, type: MemoryOpType) : RacingOperation(obj, type) {
  override fun isRacing(op: Operation): Boolean {
    if (op is MemoryOperation) {
      if (op.resource == resource) {
        return type == MemoryOpType.MEMORY_WRITE || op.type == MemoryOpType.MEMORY_WRITE
      }
    }
    return false
  }
}
