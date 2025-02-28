package org.pastalab.fray.core.concurrency.primitives

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType

abstract class LockContext(lock: Any) :
    InterruptibleContext,
    Acquirable(ResourceInfo(System.identityHashCode(lock), ResourceType.LOCK)) {
  abstract val wakingThreads: MutableMap<Long, ThreadContext>
  abstract val signalContexts: MutableSet<SignalContext>

  abstract fun addWakingThread(t: ThreadContext)

  abstract fun canLock(tid: Long): Boolean

  abstract fun lock(
      lockThread: ThreadContext,
      shouldBlock: Boolean,
      lockBecauseOfWait: Boolean,
      canInterrupt: Boolean
  ): Boolean

  abstract fun unlock(
      lockThread: ThreadContext,
      unlockBecauseOfWait: Boolean,
      earlyExit: Boolean
  ): Boolean

  abstract fun hasQueuedThreads(): Boolean

  abstract fun hasQueuedThread(tid: Long): Boolean

  abstract fun isEmpty(): Boolean

  abstract fun isLockHolder(tid: Long): Boolean

  abstract fun getNumThreadsWaitingForLockDueToSignal(): Int
}
