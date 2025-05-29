package org.pastalab.fray.core.delegates

import org.pastalab.fray.core.RunContext
import org.pastalab.fray.core.utils.HelperThread
import org.pastalab.fray.core.utils.Utils
import org.pastalab.fray.core.utils.Utils.verifyOrReport

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

  inline fun runInFrayDoneNoSkip(frayBlock: () -> Result<Unit>) {
    return runInFrayDone(frayBlock, false, "", {})
  }

  inline fun <T> runInFrayDoneWithOriginBlockAndNoSkip(
      frayBlock: () -> Result<T>,
      originBlock: () -> T
  ): T {
    return runInFrayDone(frayBlock, false, "", originBlock)
  }

  inline fun runInFrayDone(skipSignature: String, frayBlock: () -> Result<Unit>) {
    return runInFrayDone(frayBlock, true, skipSignature, {})
  }

  inline fun <T> runInFrayDoneWithOriginBlock(
      skipSignature: String,
      frayBlock: () -> Result<T>,
      originBlock: (() -> T)
  ): T {
    return runInFrayDone(frayBlock, true, skipSignature, originBlock)
  }

  inline fun <T> runInFrayDone(
      frayBlock: () -> Result<T>,
      skipDone: Boolean,
      skipSignature: String,
      originBlock: (() -> T)
  ): T {
    if (skipDone) {
      onSkipMethodDone(skipSignature)
    }
    if (checkEntered()) {
      return originBlock()
    }
    val result = frayBlock()
    entered.set(false)
    result.fold(
        onFailure = { throw it },
        onSuccess = {
          return it
        },
    )
  }

  inline fun runInFrayStartNoSkip(frayBlock: () -> Result<Unit>) {
    runInFrayStart(frayBlock, false, "", {})
  }

  inline fun runInFrayStart(skipSignature: String, frayBlock: () -> Result<Unit>) {
    runInFrayStart(frayBlock, true, skipSignature, {})
  }

  inline fun <T> runInFrayStartWithOriginBlock(
      skipSignature: String,
      frayBlock: () -> Result<T>,
      originBlock: () -> T
  ): T {
    return runInFrayStart(frayBlock, true, skipSignature, originBlock)
  }

  inline fun <T> runInFrayStart(
      frayBlock: () -> Result<T>,
      skipStart: Boolean,
      skipSignature: String,
      originBlock: () -> T
  ): T {
    if (checkEntered()) {
      if (skipStart) {
        onSkipMethod(skipSignature)
      }
      return originBlock()
    }
    val result = frayBlock()
    if (skipStart) {
      onSkipMethod(skipSignature)
    }
    entered.set(false)
    result.fold(
        onFailure = { throw it },
        onSuccess = {
          return it
        },
    )
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
