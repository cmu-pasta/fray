package cmu.pasta.sfuzz.core.concurrency.locks

import cmu.pasta.sfuzz.core.GlobalContext
import cmu.pasta.sfuzz.core.ThreadState
import cmu.pasta.sfuzz.core.concurrency.operations.ThreadResumeOperation
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock

class ReentrantReadWriteLockContext : LockContext {
  private val readLockHolder = mutableSetOf<Long>()
  private var writeLockHolder: Long? = null
  private val readLockTimes = mutableMapOf<Long, Int>()
  private val writeLockTimes = mutableMapOf<Long, Int>()
  private val readLockWaiters = mutableSetOf<Long>()
  private val writeLockWaiters = mutableSetOf<Long>()
  override val wakingThreads: MutableSet<Long> = mutableSetOf()

  override fun addWakingThread(lockObject: Any, t: Thread) {
    wakingThreads.add(t.id)
  }

  override fun canLock(tid: Long) =
      (writeLockHolder == null || writeLockHolder == tid) && readLockHolder.isEmpty()

  override fun isEmpty(): Boolean =
      writeLockHolder == null &&
          readLockHolder.isEmpty() &&
          readLockTimes.isEmpty() &&
          writeLockTimes.isEmpty() &&
          readLockWaiters.isEmpty() &&
          writeLockWaiters.isEmpty() &&
          wakingThreads.isEmpty()

  override fun lock(
      lock: Any,
      tid: Long,
      shouldBlock: Boolean,
      lockBecauseOfWait: Boolean
  ): Boolean {
    return if (lock is ReadLock) {
      readLockLock(tid, shouldBlock, lockBecauseOfWait)
    } else {
      writeLockLock(tid, shouldBlock, lockBecauseOfWait)
    }
  }

  override fun unlock(lock: Any, tid: Long, unlockBecauseOfWait: Boolean): Boolean {
    return if (lock is ReadLock) {
      readLockUnlock(tid, unlockBecauseOfWait)
    } else {
      writeLockUnlock(tid, unlockBecauseOfWait)
    }
  }

  fun readLockLock(tid: Long, shouldBlock: Boolean, lockBecauseOfWait: Boolean): Boolean {
    assert(!lockBecauseOfWait) // Read lock does not have `Condition`
    if (writeLockHolder != null && writeLockHolder != tid) {
      if (shouldBlock) {
        readLockWaiters.add(tid)
      }
      return false
    }
    readLockTimes[tid] = readLockTimes.getOrDefault(tid, 0) + 1
    readLockHolder.add(tid)
    return true
  }

  fun writeLockLock(tid: Long, shouldBlock: Boolean, lockBecauseOfWait: Boolean): Boolean {
    if ((writeLockHolder != null && writeLockHolder != tid) || readLockHolder.isNotEmpty()) {
      if (shouldBlock) {
        writeLockWaiters.add(tid)
      }
      return false
    }
    if (!lockBecauseOfWait) {
      writeLockTimes[tid] = writeLockTimes.getOrDefault(tid, 0) + 1
    }
    writeLockHolder = tid
    return true
  }

  fun readLockUnlock(tid: Long, unlockBecauseOfWait: Boolean): Boolean {
    assert(readLockHolder.contains(tid))
    assert(!unlockBecauseOfWait) // Read lock does not have `Condition`
    readLockTimes[tid] = readLockTimes[tid]!! - 1
    if (readLockTimes[tid] == 0) {
      readLockTimes.remove(tid)
      readLockHolder.remove(tid)
      if (readLockHolder.isEmpty() && writeLockHolder == null) {
        unlockAllWaiters()
        return true
      }
    }
    return false
  }

  fun writeLockUnlock(tid: Long, unlockBecauseOfWait: Boolean): Boolean {
    assert(writeLockHolder == tid)
    if (!unlockBecauseOfWait) {
      writeLockTimes[tid] = writeLockTimes[tid]!! - 1
    }
    if (writeLockTimes[tid] == 0 || unlockBecauseOfWait) {
      if (writeLockTimes[tid] == 0) {
        writeLockTimes.remove(tid)
      }
      writeLockHolder = null
      unlockReadWaiters()
      if (readLockHolder.isEmpty()) {
        unlockWriteWaiters()
        return true
      }
    }
    return false
  }

  fun unlockWriteWaiters() {
    for (writeLockWaiter in writeLockWaiters) {
      GlobalContext.registeredThreads[writeLockWaiter]!!.pendingOperation = ThreadResumeOperation()
      GlobalContext.registeredThreads[writeLockWaiter]!!.state = ThreadState.Enabled
    }
    // Waking threads are write waiters as well.
    for (thread in wakingThreads) {
      GlobalContext.registeredThreads[thread]!!.pendingOperation = ThreadResumeOperation()
      GlobalContext.registeredThreads[thread]!!.state = ThreadState.Enabled
    }
  }

  fun unlockReadWaiters() {
    for (readLockWaiter in readLockWaiters) {
      GlobalContext.registeredThreads[readLockWaiter]!!.pendingOperation = ThreadResumeOperation()
      GlobalContext.registeredThreads[readLockWaiter]!!.state = ThreadState.Enabled
    }
  }

  fun unlockAllWaiters() {
    unlockReadWaiters()
    unlockWriteWaiters()
  }
}
