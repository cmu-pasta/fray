package org.pastalab.fray.core.delegates

import kotlin.text.get
import kotlin.text.set
import org.pastalab.fray.core.RunContext
import org.pastalab.fray.core.utils.HelperThread
import org.pastalab.fray.core.utils.Utils

class DelegateSynchronizer(val context: RunContext) {
  var entered = ThreadLocal.withInitial { false }
  var skipFunctionEntered = ThreadLocal.withInitial { 0 }
  val stackTrace = ThreadLocal.withInitial { mutableListOf<String>() }
  val onSkipRecursion = ThreadLocal.withInitial { false }

  fun checkEntered(): Boolean {

    if (entered.get()) {
      return true
    }
    entered.set(true)
    if (skipFunctionEntered.get() > 0) {
      entered.set(false)
      return true
    }
    if (Thread.currentThread() is HelperThread) {
      entered.set(false)
      return true
    }
    // We do not process threads created outside of application.
    if (!context.registeredThreads.containsKey(Thread.currentThread().id)) {
      entered.set(false)
      return true
    }
    return false
  }

  fun onSkipMethod(signature: String) {
    if (onSkipRecursion.get()) {
      return
    }
    onSkipRecursion.set(true)
    stackTrace.get().add(signature)
    skipFunctionEntered.set(1 + skipFunctionEntered.get())

    if (!context.registeredThreads.containsKey(Thread.currentThread().id)) {
      stackTrace.get().removeLast()
      skipFunctionEntered.set(skipFunctionEntered.get() - 1)
    }
    onSkipRecursion.set(false)
  }

  fun onSkipMethodDone(signature: String): Boolean {
    if (onSkipRecursion.get()) {
      return false
    }
    onSkipRecursion.set(true)
    try {
      if (!context.registeredThreads.containsKey(Thread.currentThread().id)) {
        return false
      }
      Utils.verifyOrReport(!stackTrace.get().isEmpty())
      val last = stackTrace.get().removeLast()
      Utils.verifyOrReport(last == signature)
      skipFunctionEntered.set(skipFunctionEntered.get() - 1)
      return true
    } finally {
      onSkipRecursion.set(false)
    }
  }
}
