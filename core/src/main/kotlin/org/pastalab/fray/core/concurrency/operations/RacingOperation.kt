package org.pastalab.fray.core.concurrency.operations

abstract class RacingOperation(
    val resource: Int,
    val type: org.pastalab.fray.runtime.MemoryOpType
) : Operation() {
  // We use the stack trace hash to distinguish between different operations statically.
  val stackTraceHash: Int = Thread.currentThread().stackTrace.joinToString("\n").hashCode()

  abstract fun isRacing(op: Operation): Boolean
}
