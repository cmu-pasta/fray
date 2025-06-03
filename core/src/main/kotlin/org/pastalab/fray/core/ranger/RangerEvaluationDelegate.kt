package org.pastalab.fray.core.ranger

import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import org.pastalab.fray.runtime.Delegate

class RangerEvaluationDelegate(
    val rangerRunContext: RangerEvaluationContext,
    val evaluatingThread: Thread
) : Delegate() {
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
    if (Thread.currentThread() != evaluatingThread) {
      return true
    }
    entered.set(true)
    return false
  }

  override fun onConditionAwait(l: Condition) {
    if (checkEntered()) return
    entered.set(false)
    throw AbortEvaluationException("Abort ranger condition evaluation because of deadlock.")
  }

  override fun onConditionAwaitTime(condition: Condition, time: Long, unit: TimeUnit): Boolean {
    if (checkEntered()) return condition.await(time, unit)
    entered.set(false)
    throw AbortEvaluationException("Abort ranger condition evaluation because of deadlock.")
  }

  override fun onConditionAwaitNanos(condition: Condition, nanos: Long): Long {
    if (checkEntered()) return condition.awaitNanos(nanos)
    entered.set(false)
    throw AbortEvaluationException("Abort ranger condition evaluation because of deadlock.")
  }

  override fun onConditionAwaitUninterruptibly(condition: Condition) {
    if (checkEntered()) return
    entered.set(false)
    throw AbortEvaluationException("Abort ranger condition evaluation because of deadlock.")
  }

  override fun onConditionAwaitUntil(condition: Condition, deadline: Date): Boolean {
    if (checkEntered()) return condition.awaitUntil(deadline)
    entered.set(false)
    throw AbortEvaluationException("Abort ranger condition evaluation because of deadlock.")
  }

  override fun onObjectWait(o: Any, timeout: Long) {
    if (checkEntered()) return
    entered.set(false)
    throw AbortEvaluationException("Abort ranger condition evaluation because of deadlock.")
  }

  override fun onLatchAwait(latch: CountDownLatch) {
    if (checkEntered()) return
    try {
      rangerRunContext.latchAwait(latch)
    } finally {
      entered.set(false)
    }
  }

  override fun onMonitorEnter(o: Any) {
    if (checkEntered()) return
    try {
      rangerRunContext.lockImpl(o)
    } finally {
      entered.set(false)
    }
  }

  override fun onLockLock(l: Lock) {
    if (checkEntered()) {
      onSkipMethod("Lock.lock")
      return
    }
    try {
      rangerRunContext.lockImpl(l)
    } finally {
      entered.set(false)
      onSkipMethod("Lock.lock")
    }
  }

  override fun onLockLockDone() {
    onSkipMethodDone("Lock.lock")
  }

  override fun onLockTryLockInterruptibly(l: Lock, timeout: Long, unit: TimeUnit): Long {
    if (checkEntered()) {
      onSkipMethod("Lock.tryLock")
      return unit.toMillis(timeout)
    }
    try {
      rangerRunContext.lockImpl(l)
    } finally {
      entered.set(false)
      onSkipMethod("Lock.tryLock")
    }
    return 0
  }

  override fun onLockTryLockInterruptiblyDone(l: Lock) {
    onSkipMethodDone("Lock.tryLock")
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
