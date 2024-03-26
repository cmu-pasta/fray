package cmu.pasta.sfuzz.core.concurrency.operations

import cmu.pasta.sfuzz.runtime.MemoryOpType

class ObjectWaitOperation(obj: Int) : RacingOperation(obj, MemoryOpType.MEMORY_WRITE) {
  override fun isRacing(op: Operation): Boolean {
    if (op is ObjectWaitOperation) {
      return op.resource == resource
    }
    return false
  }
}
