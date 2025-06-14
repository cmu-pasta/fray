package org.pastalab.fray.core.delegates

import org.pastalab.fray.core.RunContext
import org.pastalab.fray.core.utils.HelperThread
import org.pastalab.fray.core.utils.Utils

class DelegateSynchronizer(val context: RunContext) {
  val entered: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

  val skipPrimitiveEntered: ThreadLocal<Int> = ThreadLocal.withInitial { 0 }
  val skipPrimitiveStackTrace: ThreadLocal<MutableList<String>> =
      ThreadLocal.withInitial { mutableListOf() }
  val onSkipRecursion: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

  val skipSchedulingEntered: ThreadLocal<Int> = ThreadLocal.withInitial { 0 }
  val skipSchedulingStackTrace: ThreadLocal<MutableList<String>> =
      ThreadLocal.withInitial { mutableListOf() }

  fun checkEntered(): Boolean {
    if (entered.get()) {
      return true
    }
    entered.set(true)
    if (skipPrimitiveEntered.get() > 0) {
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
      onSkipPrimitiveDone(skipSignature)
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
        onSkipPrimitive(skipSignature)
      }
      return originBlock()
    }
    val result = frayBlock()
    if (skipStart) {
      onSkipPrimitive(skipSignature)
    }
    entered.set(false)
    result.fold(
        onFailure = { throw it },
        onSuccess = {
          return it
        },
    )
  }

  /**
   * Fray provides two ways to skip instrumented primitive: [onSkipPrimitive] and
   * [onSkipScheduling]. [onSkipPrimitive] skips the primitives completely, meaning that [Fray] will
   * not run instrumented code at all. This method should be used if the primitive is modeled
   * through Fray, and we want to skip its internal implementation (e.g.,
   * [java.util.concurrent.locks.ReentrantLock]). [onSkipScheduling] on the other hand, still runs
   * the thread with Fray instrumentation, but it tries to prioritize the thread without scheduling
   * other threads. This method is useful when a thread enters [<clinit>] or class loaders.
   */
  fun onSkipPrimitive(signature: String) {
    if (onSkipRecursion.get()) {
      return
    }
    onSkipRecursion.set(true)
    skipPrimitiveStackTrace.get().add(signature)
    skipPrimitiveEntered.set(1 + skipPrimitiveEntered.get())

    if (!context.registeredThreads.containsKey(Thread.currentThread().id)) {
      skipPrimitiveStackTrace.get().removeLast()
      skipPrimitiveEntered.set(skipPrimitiveEntered.get() - 1)
    }
    onSkipRecursion.set(false)
  }

  fun onSkipPrimitiveDone(signature: String): Boolean {
    if (onSkipRecursion.get()) {
      return false
    }
    onSkipRecursion.set(true)
    try {
      if (!context.registeredThreads.containsKey(Thread.currentThread().id)) {
        return false
      }
      Utils.verifyOrReport(!skipPrimitiveStackTrace.get().isEmpty())
      val last = skipPrimitiveStackTrace.get().removeLast()
      Utils.verifyOrReport(last == signature)
      skipPrimitiveEntered.set(skipPrimitiveEntered.get() - 1)
      return true
    } finally {
      onSkipRecursion.set(false)
    }
  }

  fun onSkipScheduling(signature: String) = runInFrayStartNoSkip {
    skipSchedulingEntered.set(1 + skipSchedulingEntered.get())
    skipSchedulingStackTrace.get().add(signature)
    val threadContext = context.registeredThreads[Thread.currentThread().id]!!
    context.prioritizedThreads.add(threadContext)
    return@runInFrayStartNoSkip Result.success(Unit)
  }

  fun onSkipSchedulingDone(signature: String) = runInFrayDoneNoSkip {
    Utils.verifyOrReport(!skipSchedulingStackTrace.get().isEmpty())
    val last = skipSchedulingStackTrace.get().removeLast()
    Utils.verifyOrReport(last == signature)
    skipSchedulingEntered.set(skipSchedulingEntered.get() - 1)
    if (skipSchedulingEntered.get() == 0) {
      val threadContext = context.registeredThreads[Thread.currentThread().id]!!
      context.prioritizedThreads.remove(threadContext)
    }
    return@runInFrayDoneNoSkip Result.success(Unit)
  }
}
