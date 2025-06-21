package org.pastalab.fray.core.concurrency.operations

abstract class RacingOperation(
    val resource: Int,
    val type: org.pastalab.fray.runtime.MemoryOpType
) : Operation() {

  val stackTraceHash: Int =
      if (resolveRacingOperationStackTraceHash) {
        RuntimeException().stackTrace.joinToString("\n").hashCode()
      } else {
        0
      }

  abstract fun isRacing(op: Operation): Boolean

  companion object {
    val resolveRacingOperationStackTraceHash =
        System.getProperty("fray.resolveRacingOperationStackTraceHash")?.toBoolean() ?: false
  }
}
