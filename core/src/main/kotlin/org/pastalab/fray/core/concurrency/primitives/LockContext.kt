package org.pastalab.fray.core.concurrency.primitives

import org.pastalab.fray.core.ThreadContext

interface LockContext : InterruptibleContext {
  val wakingThreads: MutableMap<Long, ThreadContext>
  val signalContexts: MutableSet<SignalContext>

  fun addWakingThread(t: ThreadContext)

  fun canLock(tid: Long): Boolean

  fun lock(
      lockThread: ThreadContext,
      shouldBlock: Boolean,
      lockBecauseOfWait: Boolean,
      canInterrupt: Boolean
  ): Boolean

  fun unlock(tid: Long, unlockBecauseOfWait: Boolean, earlyExit: Boolean): Boolean

  fun hasQueuedThreads(): Boolean

  fun hasQueuedThread(tid: Long): Boolean

  fun isEmpty(): Boolean

  fun isLockHolder(tid: Long): Boolean

  fun getNumThreadsWaitingForLockDueToSignal(): Int
}
