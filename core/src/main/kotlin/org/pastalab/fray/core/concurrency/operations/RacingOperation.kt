package org.pastalab.fray.core.concurrency.operations

abstract class RacingOperation(
    val resource: Int,
    val type: org.pastalab.fray.runtime.MemoryOpType
) : Operation {
  abstract fun isRacing(op: Operation): Boolean
}
