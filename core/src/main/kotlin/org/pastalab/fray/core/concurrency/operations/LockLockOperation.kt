package org.anonlab.fray.core.concurrency.operations

class LockLockOperation(obj: Any) :
    RacingOperation(
        System.identityHashCode(obj), org.anonlab.fray.runtime.MemoryOpType.MEMORY_WRITE) {
  val name = obj.javaClass.name

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
