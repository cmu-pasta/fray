package org.anonlab.fray.core.utils

import kotlin.math.ceil
import kotlin.math.ln
import org.anonlab.fray.core.FrayInternalError
import org.anonlab.fray.core.ThreadContext
import org.anonlab.fray.rmi.ThreadInfo
import org.anonlab.fray.runtime.Runtime

object Utils {
  fun sampleGeometric(p: Double, rand: Double): Int {
    return ceil(ln(1 - rand) / ln(1 - p)).toInt()
  }

  fun verifyOrReport(condition: Boolean) {
    verifyOrReport(condition, "Internal error")
  }

  fun verifyOrReport(condition: Boolean, message: String) {
    if (!condition) {
      val e = FrayInternalError(message)
      Runtime.onReportError(e)
    }
  }

  fun verifyOrReport(condition: Boolean, message: () -> String) {
    verifyOrReport(condition, message())
  }
}

val StackTraceElement.isFrayInternals
  get() =
      this.className.startsWith("org.anonlab.fray.core") ||
          this.className.startsWith("org.anonlab.fray.runtime")

fun List<ThreadContext>.toThreadInfos(): List<ThreadInfo> {
  return this.map { it.toThreadInfo() }
}
