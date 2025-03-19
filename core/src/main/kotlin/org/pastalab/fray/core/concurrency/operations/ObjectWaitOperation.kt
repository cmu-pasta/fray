package org.anonlab.fray.core.concurrency.operations

class ObjectWaitOperation(obj: Int) :
    RacingOperation(obj, org.anonlab.fray.runtime.MemoryOpType.MEMORY_WRITE) {
  override fun isRacing(op: Operation): Boolean {
    if (op is ObjectWaitOperation) {
      return op.resource == resource
    }
    return false
  }
}
