package cmu.pasta.fray.core.concurrency.locks

import cmu.pasta.fray.core.ThreadContext

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
}
