package cmu.pasta.fray.core.concurrency.locks

class StampedLockContext : LockContext {
  override val wakingThreads: MutableSet<Long> = mutableSetOf()

  override fun addWakingThread(lockObject: Any, t: Thread) {
    TODO("Not yet implemented")
  }

  override fun canLock(tid: Long): Boolean {
    TODO("Not yet implemented")
  }

  override fun lock(
      lock: Any,
      tid: Long,
      shouldBlock: Boolean,
      lockBecauseOfWait: Boolean,
      canInterrupt: Boolean
  ): Boolean {
    TODO("Not yet implemented")
  }

  override fun interrupt(tid: Long) {}

  override fun unlock(lock: Any, tid: Long, unlockBecauseOfWait: Boolean): Boolean {
    TODO("Not yet implemented")
  }

  override fun isEmpty(): Boolean {
    TODO("Not yet implemented")
  }
}
