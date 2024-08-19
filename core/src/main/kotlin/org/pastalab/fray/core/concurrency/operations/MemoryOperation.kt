package org.pastalab.fray.core.concurrency.operations

class MemoryOperation(obj: Any, type: org.pastalab.fray.runtime.MemoryOpType) :
    RacingOperation(System.identityHashCode(obj), type) {
  val name = obj.javaClass.name

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
    return super.toString() + "@$name:$type"
  }
}
