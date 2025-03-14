package org.pastalab.fray.core.concurrency.operations

class MemoryOperation(obj: Int, type: org.pastalab.fray.runtime.MemoryOpType) :
    RacingOperation(obj, type) {

  override fun isRacing(op: Operation): Boolean {
    if (op is MemoryOperation) {
      if (op.resource == resource) {
        return type == org.pastalab.fray.runtime.MemoryOpType.MEMORY_WRITE ||
            op.type == org.pastalab.fray.runtime.MemoryOpType.MEMORY_WRITE
      }
    }
    return false
  }

  override fun toString(): String {
    return super.toString() + "@$resource:$type"
  }
}
