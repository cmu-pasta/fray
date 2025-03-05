package org.pastalab.fray.core.syncurity

import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import org.pastalab.fray.runtime.Delegate

class SyncurityEvaluationDelegate(val syncurityRunContext: SyncurityEvaluationContext) :
    Delegate() {
  var entered = ThreadLocal.withInitial { false }
  var skipFunctionEntered = ThreadLocal.withInitial { 0 }
  val stackTrace = ThreadLocal.withInitial { mutableListOf<String>() }

  private fun checkEntered(): Boolean {
    if (skipFunctionEntered.get() > 0) {
      return true
    }

    if (entered.get()) {
      return true
    }
    entered.set(true)
    return false
  }

  override fun onThreadPark() {
    throw AbortEvaluation("Abort syncurity condition evaluation because of deadlock.")
  }

  override fun onConditionAwait(l: Condition) {
    throw AbortEvaluation("Abort syncurity condition evaluation because of deadlock.")
  }

  override fun onConditionAwaitTime(condition: Condition, time: Long, unit: TimeUnit?): Boolean {
    throw AbortEvaluation("Abort syncurity condition evaluation because of deadlock.")
  }

  override fun onConditionAwaitNanos(condition: Condition, nanos: Long): Long {
    throw AbortEvaluation("Abort syncurity condition evaluation because of deadlock.")
  }

  override fun onConditionAwaitUninterruptibly(condition: Condition) {
    throw AbortEvaluation("Abort syncurity condition evaluation because of deadlock.")
  }

  override fun onConditionAwaitUntil(condition: Condition, deadline: Date?): Boolean {
    throw AbortEvaluation("Abort syncurity condition evaluation because of deadlock.")
  }

  override fun onObjectWait(o: Any?, timeout: Long) {
    throw AbortEvaluation("Abort syncurity condition evaluation because of deadlock.")
  }

  override fun onLatchAwait(latch: CountDownLatch) {
    if (checkEntered()) return
    try {
      syncurityRunContext.latchAwait(latch)
    } finally {
      entered.set(false)
    }
  }

  override fun onMonitorEnter(o: Any) {
    if (checkEntered()) return
    try {
      syncurityRunContext.lockImpl(o)
    } finally {
      entered.set(false)
    }
  }

  val onSkipRecursion = ThreadLocal.withInitial { false }

  override fun onSkipMethod(signature: String) {
    if (onSkipRecursion.get()) {
      return
    }
    onSkipRecursion.set(true)
    stackTrace.get().add(signature)
    skipFunctionEntered.set(1 + skipFunctionEntered.get())
    onSkipRecursion.set(false)
  }

  override fun onSkipMethodDone(signature: String): Boolean {
    if (onSkipRecursion.get()) {
      return false
    }
    onSkipRecursion.set(true)
    try {
      if (stackTrace.get().isEmpty()) {
        return false
      }
      val last = stackTrace.get().removeLast()
      if (last != signature) {
        return false
      }
      skipFunctionEntered.set(skipFunctionEntered.get() - 1)
      return true
    } finally {
      onSkipRecursion.set(false)
    }
  }
}
