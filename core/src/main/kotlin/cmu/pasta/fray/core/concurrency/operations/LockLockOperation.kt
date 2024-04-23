package cmu.pasta.fray.core.concurrency.operations

import cmu.pasta.fray.runtime.MemoryOpType

class LockLockOperation(obj: Int) : RacingOperation(obj, MemoryOpType.MEMORY_WRITE) {
  override fun isRacing(op: Operation): Boolean {
    if (op is LockLockOperation) {
      return op.resource == resource
    }
    return false
  }
}
