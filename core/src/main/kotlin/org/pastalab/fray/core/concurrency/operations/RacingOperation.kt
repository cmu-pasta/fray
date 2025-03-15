package org.pastalab.fray.core.concurrency.operations

abstract class RacingOperation(
    val resource: Int,
    val type: org.pastalab.fray.runtime.MemoryOpType
) : Operation() {
  // Well this does not completely solve the performance issue.
  // https://www.baeldung.com/java-exceptions-performance
  private val dummyThrowable = RuntimeException()

  // TODO(aoli): we should find a more efficient solution.
  // We use the stack trace hash to distinguish between different operations statically.
  val stackTraceHash: Int by lazy { dummyThrowable.stackTrace.joinToString("\n").hashCode() }

  abstract fun isRacing(op: Operation): Boolean
}
