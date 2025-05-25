package org.pastalab.fray.core.utils

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.ceil
import kotlin.math.ln
import org.pastalab.fray.core.FrayInternalError
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.rmi.ThreadInfo
import org.pastalab.fray.runtime.Runtime

object Utils {
  fun sampleGeometric(p: Double, rand: Double): Int {
    return ceil(ln(1 - rand) / ln(1 - p)).toInt()
  }

  @OptIn(ExperimentalContracts::class)
  fun verifyOrReport(condition: Boolean) {
    contract { returns() implies condition }
    verifyOrReport(condition, "Internal error")
  }

  @OptIn(ExperimentalContracts::class)
  fun verifyOrReport(condition: Boolean, message: String) {
    contract { returns() implies condition }
    verifyOrReport(condition) { message }
  }

  @OptIn(ExperimentalContracts::class)
  fun verifyOrReport(condition: Boolean, message: () -> String) {
    contract { returns() implies condition }
    if (!condition) {
      val e = FrayInternalError(message())
      Runtime.onReportError(e)
    }
  }

  internal fun <T> verifyNoThrow(block: () -> T): Result<T> {
    val result = mustBeCaught { block() }
    verifyOrReport(result.isSuccess) {
      val exception = result.exceptionOrNull()!!
      "Expected no exception, but got: $exception, stack trace: ${exception.stackTrace.joinToString("\n")}"
    }
    return result
  }

  internal inline fun <T> mustBeCaught(block: () -> T): Result<T> {
    return runCatching { block() }
  }
}

val StackTraceElement.isFrayInternals
  get() =
      this.className.startsWith("org.pastalab.fray.core") ||
          this.className.startsWith("org.pastalab.fray.runtime")

fun List<ThreadContext>.toThreadInfos(): List<ThreadInfo> {
  return this.map { it.toThreadInfo() }
}
