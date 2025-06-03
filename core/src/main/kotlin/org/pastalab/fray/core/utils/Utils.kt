package org.pastalab.fray.core.utils

import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.ceil
import kotlin.math.ln
import kotlinx.serialization.json.Json
import org.pastalab.fray.core.FrayInternalError
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.scheduler.Scheduler
import org.pastalab.fray.rmi.ThreadInfo
import org.pastalab.fray.runtime.Runtime
import org.pastalab.fray.runtime.TargetTerminateException

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
    verifyOrReport(result.isSuccess || result.exceptionOrNull() is TargetTerminateException) {
      val exception = result.exceptionOrNull()!!
      "Expected no exception, but got: $exception, stack trace: ${exception.stackTrace.joinToString("\n")}"
    }
    return result
  }

  internal inline fun <T> mustBeCaught(block: () -> T): Result<T> {
    val result = runCatching { block() }
    if (result.isFailure) {
      val exception = result.exceptionOrNull()
      verifyOrReport(exception is InterruptedException || exception is TargetTerminateException)
    }
    return result
  }
}

val StackTraceElement.isFrayInternals
  get() =
      this.className.startsWith("org.pastalab.fray.core") ||
          this.className.startsWith("org.pastalab.fray.runtime")

fun List<ThreadContext>.toThreadInfos(): List<ThreadInfo> {
  return this.map { it.toThreadInfo() }
}

fun schedulerFromRecording(path: String): Scheduler {
  val schedulerPath = "$path/schedule.json"
  return Json.decodeFromString<Scheduler>(File(schedulerPath).readText())
}

fun randomFromRecording(path: String): ControlledRandom {
  val randomPath = "$path/random.json"
  return Json.decodeFromString<ControlledRandom>(File(randomPath).readText())
}
