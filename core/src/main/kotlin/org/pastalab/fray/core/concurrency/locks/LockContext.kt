package org.pastalab.fray.core.concurrency.locks

import org.pastalab.fray.core.ThreadContext

interface LockContext : Interruptible {
  val wakingThreads: MutableMap<Long, ThreadContext>

  fun addWakingThread(lockObject: Any, t: ThreadContext)

  fun canLock(tid: Long): Boolean

  fun lock(
      lock: Any,
      lockThread: ThreadContext,
      shouldBlock: Boolean,
      lockBecauseOfWait: Boolean,
      canInterrupt: Boolean
  ): Boolean

  fun unlock(lock: Any, tid: Long, unlockBecauseOfWait: Boolean, earlyExit: Boolean): Boolean

  fun hasQueuedThreads(): Boolean

  fun hasQueuedThread(tid: Long): Boolean

  fun isEmpty(): Boolean

  fun isLockHolder(lock: Any, tid: Long): Boolean

  fun tryLockUnblocked(lock: Any, tid: Long)
}
