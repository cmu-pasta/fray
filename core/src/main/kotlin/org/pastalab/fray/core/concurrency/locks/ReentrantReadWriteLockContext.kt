package org.pastalab.fray.core.concurrency.locks

import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.ThreadState
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation

class ReentrantReadWriteLockContext : LockContext {
  private val readLockHolder = mutableSetOf<Long>()
  private var writeLockHolder: Long? = null
  private val readLockTimes = mutableMapOf<Long, Int>()
  private val writeLockTimes = mutableMapOf<Long, Int>()
  override val wakingThreads = mutableMapOf<Long, ThreadContext>()

  private val readLockWaiters = mutableMapOf<Long, LockWaiter>()
  private val writeLockWaiters = mutableMapOf<Long, LockWaiter>()

  override fun hasQueuedThreads(): Boolean {
    return writeLockWaiters.any() || wakingThreads.any() || readLockWaiters.any()
  }

  override fun hasQueuedThread(tid: Long): Boolean {
    return writeLockWaiters.containsKey(tid) ||
        wakingThreads.contains(tid) ||
        readLockWaiters.containsKey(tid)
  }

  override fun addWakingThread(lockObject: Any, t: ThreadContext) {
    wakingThreads[t.thread.id] = t
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
      lockThread: ThreadContext,
      shouldBlock: Boolean,
      lockBecauseOfWait: Boolean,
      canInterrupt: Boolean,
  ): Boolean {
    return if (lock is ReadLock) {
      readLockLock(lockThread, shouldBlock, lockBecauseOfWait, canInterrupt)
    } else {
      writeLockLock(lockThread, shouldBlock, lockBecauseOfWait, canInterrupt)
    }
  }

  override fun interrupt(tid: Long) {
    val writeLockWaiter = writeLockWaiters[tid]
    if (writeLockWaiter != null && writeLockWaiter.canInterrupt) {
      writeLockWaiter.thread.pendingOperation = ThreadResumeOperation(false)
      writeLockWaiter.thread.state = ThreadState.Enabled
      writeLockWaiters.remove(tid)
    }
    val readLockWaiter = readLockWaiters[tid]
    if (readLockWaiter != null && readLockWaiter.canInterrupt) {
      readLockWaiter.thread.pendingOperation = ThreadResumeOperation(false)
      readLockWaiter.thread.state = ThreadState.Enabled
      readLockWaiters.remove(tid)
    }
  }

  override fun unlock(
      lock: Any,
      tid: Long,
      unlockBecauseOfWait: Boolean,
      earlyExit: Boolean
  ): Boolean {
    return if (lock is ReadLock) {
      readLockUnlock(tid, unlockBecauseOfWait)
    } else {
      writeLockUnlock(tid, unlockBecauseOfWait)
    }
  }

  fun readLockLock(
      lockThread: ThreadContext,
      shouldBlock: Boolean,
      lockBecauseOfWait: Boolean,
      canInterrupt: Boolean
  ): Boolean {
    assert(!lockBecauseOfWait) // Read lock does not have `Condition`
    val tid = lockThread.thread.id
    if (writeLockHolder != null && writeLockHolder != tid) {
      if (canInterrupt) {
        lockThread.checkInterrupt()
      }
      if (shouldBlock) {
        readLockWaiters[tid] = LockWaiter(canInterrupt, lockThread)
      }
      return false
    }
    readLockTimes[tid] = readLockTimes.getOrDefault(tid, 0) + 1
    readLockHolder.add(tid)
    return true
  }

  fun writeLockLock(
      lockThread: ThreadContext,
      shouldBlock: Boolean,
      lockBecauseOfWait: Boolean,
      canInterrupt: Boolean
  ): Boolean {
    val tid = lockThread.thread.id
    if ((writeLockHolder != null && writeLockHolder != tid) || readLockHolder.isNotEmpty()) {
      if (canInterrupt) {
        lockThread.checkInterrupt()
      }
      if (shouldBlock) {
        writeLockWaiters[tid] = LockWaiter(canInterrupt, lockThread)
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
    for (writeLockWaiter in writeLockWaiters.values) {
      writeLockWaiter.thread.pendingOperation = ThreadResumeOperation(true)
      writeLockWaiter.thread.state = ThreadState.Enabled
    }
    // Waking threads are write waiters as well.
    for (thread in wakingThreads.values) {
      thread.pendingOperation = ThreadResumeOperation(true)
      thread.state = ThreadState.Enabled
    }
  }

  fun unlockReadWaiters() {
    for (readLockWaiter in readLockWaiters.values) {
      readLockWaiter.thread.pendingOperation = ThreadResumeOperation(true)
      readLockWaiter.thread.state = ThreadState.Enabled
    }
  }

  fun unlockAllWaiters() {
    unlockReadWaiters()
    unlockWriteWaiters()
  }

  override fun isLockHolder(lock: Any, tid: Long): Boolean {
    return if (lock is ReadLock) {
      readLockHolder.contains(tid)
    } else {
      writeLockHolder == tid
    }
  }
}
