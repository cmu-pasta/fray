package org.pastalab.fray.core.utils

import kotlin.math.ceil
import kotlin.math.ln
import org.pastalab.fray.core.FrayInternalError
import org.pastalab.fray.runtime.Runtime

object Utils {
  fun sampleGeometric(p: Double, rand: Double): Int {
    return ceil(ln(1 - rand) / ln(1 - p)).toInt()
  }

  fun verifyOrReport(condition: Boolean) {
    if (!condition) {
      val e = FrayInternalError("Internal error")
      Runtime.onReportError(e)
    }
  }

  fun verifyOrReport(condition: Boolean, message: String) {
    if (!condition) {
      val e = FrayInternalError(message)
      Runtime.onReportError(e)
    }
  }

  fun verifyOrReport(condition: Boolean, message: () -> String) {
    if (!condition) {
      val e = FrayInternalError(message())
      Runtime.onReportError(e)
    }
  }
}

val StackTraceElement.isFrayInternals
  get() =
      this.className.startsWith("org.pastalab.fray.core") ||
          this.className.startsWith("org.pastalab.fray.runtime")
