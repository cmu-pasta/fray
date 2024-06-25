package cmu.pasta.fray.core.concurrency.locks

import cmu.pasta.fray.core.GlobalContext
import cmu.pasta.fray.core.ThreadState
import cmu.pasta.fray.core.concurrency.operations.ThreadResumeOperation
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock

class ReentrantReadWriteLockContext : LockContext {
  private val readLockHolder = mutableSetOf<Long>()
  private var writeLockHolder: Long? = null
  private val readLockTimes = mutableMapOf<Long, Int>()
  private val writeLockTimes = mutableMapOf<Long, Int>()
  override val wakingThreads: MutableSet<Long> = mutableSetOf()

  private val readLockWaiters = mutableMapOf<Long, Boolean>()
  private val writeLockWaiters = mutableMapOf<Long, Boolean>()

  override fun hasQueuedThreads(): Boolean {
    return writeLockWaiters.any() || wakingThreads.any() || readLockWaiters.any()
  }

  override fun hasQueuedThread(tid: Long): Boolean {
    return writeLockWaiters.containsKey(tid) ||
        wakingThreads.contains(tid) ||
        readLockWaiters.containsKey(tid)
  }

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
      lockBecauseOfWait: Boolean,
      canInterrupt: Boolean,
  ): Boolean {
    return if (lock is ReadLock) {
      readLockLock(tid, shouldBlock, lockBecauseOfWait, canInterrupt)
    } else {
      writeLockLock(tid, shouldBlock, lockBecauseOfWait, canInterrupt)
    }
  }

  override fun interrupt(tid: Long) {
    if (writeLockWaiters[tid] == true) {
      GlobalContext.registeredThreads[tid]!!.pendingOperation = ThreadResumeOperation()
      GlobalContext.registeredThreads[tid]!!.state = ThreadState.Enabled
      writeLockWaiters.remove(tid)
    }
    if (readLockWaiters[tid] == true) {
      GlobalContext.registeredThreads[tid]!!.pendingOperation = ThreadResumeOperation()
      GlobalContext.registeredThreads[tid]!!.state = ThreadState.Enabled
      readLockWaiters.remove(tid)
    }
  }

  override fun unlock(lock: Any, tid: Long, unlockBecauseOfWait: Boolean): Boolean {
    return if (lock is ReadLock) {
      readLockUnlock(tid, unlockBecauseOfWait)
    } else {
      writeLockUnlock(tid, unlockBecauseOfWait)
    }
  }

  fun readLockLock(
      tid: Long,
      shouldBlock: Boolean,
      lockBecauseOfWait: Boolean,
      canInterrupt: Boolean
  ): Boolean {
    assert(!lockBecauseOfWait) // Read lock does not have `Condition`
    if (writeLockHolder != null && writeLockHolder != tid) {
      if (canInterrupt) {
        GlobalContext.registeredThreads[tid]?.checkInterrupt()
      }
      if (shouldBlock) {
        readLockWaiters[tid] = canInterrupt
      }
      return false
    }
    readLockTimes[tid] = readLockTimes.getOrDefault(tid, 0) + 1
    readLockHolder.add(tid)
    return true
  }

  fun writeLockLock(
      tid: Long,
      shouldBlock: Boolean,
      lockBecauseOfWait: Boolean,
      canInterrupt: Boolean
  ): Boolean {
    if ((writeLockHolder != null && writeLockHolder != tid) || readLockHolder.isNotEmpty()) {
      if (canInterrupt) {
        GlobalContext.registeredThreads[tid]?.checkInterrupt()
      }
      if (shouldBlock) {
        writeLockWaiters[tid] = canInterrupt
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
    for (writeLockWaiter in writeLockWaiters.keys) {
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
    for (readLockWaiter in readLockWaiters.keys) {
      GlobalContext.registeredThreads[readLockWaiter]!!.pendingOperation = ThreadResumeOperation()
      GlobalContext.registeredThreads[readLockWaiter]!!.state = ThreadState.Enabled
    }
  }

  fun unlockAllWaiters() {
    unlockReadWaiters()
    unlockWriteWaiters()
  }
}
