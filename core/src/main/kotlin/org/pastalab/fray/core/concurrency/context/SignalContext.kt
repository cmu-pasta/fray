package org.pastalab.fray.core.concurrency.context

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.InterruptionType
import org.pastalab.fray.core.randomness.Randomness
import org.pastalab.fray.core.utils.Utils.verifyOrReport
import org.pastalab.fray.rmi.ThreadState

abstract class SignalContext(val lockContext: LockContext) : InterruptibleContext {
  val waitingThreads = mutableListOf<ThreadContext>()

  abstract fun getSyncObject(): Any

  abstract fun sendSignalToObject()

  abstract fun updatedThreadContextDueToUnblock(
      threadContext: ThreadContext,
      type: InterruptionType,
  )

  abstract fun updateThreadContextDueToBlock(
      threadContext: ThreadContext,
      blockedUntil: Long,
      canInterrupt: Boolean,
  )

  fun addWaitingThread(threadContext: ThreadContext, blockedUntil: Long, canInterrupt: Boolean) {
    verifyOrReport { threadContext !in waitingThreads }
    waitingThreads.add(threadContext)
    updateThreadContextDueToBlock(threadContext, blockedUntil, canInterrupt)
  }

  override fun unblockThread(tid: Long, type: InterruptionType): Boolean {
    val threadContext = waitingThreads.find { it.thread.id == tid } ?: return false
    verifyOrReport({ threadContext in waitingThreads }) {
      "Thread $threadContext is not waiting on this signal"
    }
    waitingThreads.remove(threadContext)
    updatedThreadContextDueToUnblock(threadContext, type)
    lockContext.addWakingThread(threadContext)
    if (lockContext.canLock(tid)) {
      threadContext.state = ThreadState.Runnable
      return true
    }
    return false
  }

  fun signal(randomnessProvider: Randomness, all: Boolean) {
    if (waitingThreads.isNotEmpty()) {
      if (all) {
        waitingThreads
            .map { it.thread.id }
            .forEach { unblockThread(it, InterruptionType.RESOURCE_AVAILABLE) }
      } else {
        val index = randomnessProvider.nextInt() % waitingThreads.size
        val t = waitingThreads[index]
        unblockThread(t.thread.id, InterruptionType.RESOURCE_AVAILABLE)
      }
    }
  }
}
