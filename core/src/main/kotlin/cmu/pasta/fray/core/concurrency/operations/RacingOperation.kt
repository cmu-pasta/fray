package cmu.pasta.fray.core.concurrency.operations

import cmu.pasta.fray.runtime.MemoryOpType

abstract class RacingOperation(val resource: Int, val type: MemoryOpType) : Operation {
  abstract fun isRacing(op: Operation): Boolean
}
