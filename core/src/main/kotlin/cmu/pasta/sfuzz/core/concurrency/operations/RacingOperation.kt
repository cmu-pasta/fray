package cmu.pasta.sfuzz.core.concurrency.operations

import cmu.pasta.sfuzz.runtime.MemoryOpType

abstract class RacingOperation(val resource: Int, val type: MemoryOpType) : Operation {
  abstract fun isRacing(op: Operation): Boolean
}
