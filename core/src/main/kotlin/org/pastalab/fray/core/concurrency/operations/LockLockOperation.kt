package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.runtime.MemoryOpType

class LockLockOperation(obj: Any) :
    RacingOperation(System.identityHashCode(obj), MemoryOpType.MEMORY_WRITE) {
  val name: String = obj.javaClass.name

  override fun isRacing(op: Operation): Boolean {
    if (op is LockLockOperation) {
      return op.resource == resource
    }
    return false
  }

  override fun toString(): String {
    return super.toString() + "@$name:$type"
  }
}
