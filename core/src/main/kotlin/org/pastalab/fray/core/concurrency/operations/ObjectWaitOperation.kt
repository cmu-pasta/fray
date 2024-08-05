package org.pastalab.fray.core.concurrency.operations

class ObjectWaitOperation(obj: Int) :
    RacingOperation(obj, org.pastalab.fray.runtime.MemoryOpType.MEMORY_WRITE) {
  override fun isRacing(op: Operation): Boolean {
    if (op is ObjectWaitOperation) {
      return op.resource == resource
    }
    return false
  }
}
