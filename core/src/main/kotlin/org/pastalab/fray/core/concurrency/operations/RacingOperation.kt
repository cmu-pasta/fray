package org.pastalab.fray.core.concurrency.operations

import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream

abstract class RacingOperation(
    val resource: Int,
    val type: org.pastalab.fray.runtime.MemoryOpType,
) : Operation() {

  val stackTraceHash: Int = resolveStackTraceHash()

  abstract fun isRacing(op: Operation): Boolean

  companion object {
    var resolveRacingOperationStackTraceHash = false

    private val stackWalker = StackWalker.getInstance(setOf(), 16)

    fun resolveStackTraceHash(): Int {
      if (!resolveRacingOperationStackTraceHash) return 0
      val frames =
          stackWalker.walk(
              Function<Stream<StackWalker.StackFrame>, List<String>> { stream ->
                stream
                    .filter { frame ->
                      !frame.className.startsWith("org.pastalab.fray.core") &&
                          !frame.className.startsWith("org.pastalab.fray.runtime") &&
                          !frame.className.startsWith("org.pastalab.fray.instrumentation")
                    }
                    .limit(2)
                    .map { "${it.className}.${it.methodName}:${it.lineNumber}" }
                    .collect(Collectors.toList())
              }
          )
      return frames.joinToString("\n").hashCode()
    }
  }
}
