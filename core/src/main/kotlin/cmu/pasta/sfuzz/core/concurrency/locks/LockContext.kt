package cmu.pasta.sfuzz.core.concurrency.locks

interface LockContext {
  val wakingThreads: MutableSet<Long>

  fun addWakingThread(lockObject: Any, t: Thread)

  fun canLock(tid: Long): Boolean

  fun lock(lock: Any, tid: Long, shouldBlock: Boolean, lockBecauseOfWait: Boolean): Boolean

  fun unlock(lock: Any, tid: Long, unlockBecauseOfWait: Boolean): Boolean

  fun isEmpty(): Boolean
}
