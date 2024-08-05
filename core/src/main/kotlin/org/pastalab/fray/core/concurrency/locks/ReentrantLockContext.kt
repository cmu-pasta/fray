package org.pastalab.fray.core.concurrency.locks

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.ThreadState
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation

class ReentrantLockContext : LockContext {
  private var lockHolder: Long? = null
  private val lockTimes = mutableMapOf<Long, Int>()
  // Mapping from thread id to whether the thread is interruptible.
  private val lockWaiters = mutableMapOf<Long, LockWaiter>()
  override val wakingThreads = mutableMapOf<Long, ThreadContext>()

  override fun addWakingThread(lockObject: Any, t: ThreadContext) {
    wakingThreads[t.thread.id] = t
  }

  override fun canLock(tid: Long) = lockHolder == null || lockHolder == tid

  override fun isEmpty(): Boolean =
      lockHolder == null && lockTimes.isEmpty() && lockWaiters.isEmpty() && wakingThreads.isEmpty()

  override fun hasQueuedThreads(): Boolean {
    return lockWaiters.any() || wakingThreads.any()
  }

  override fun hasQueuedThread(tid: Long): Boolean {
    return lockWaiters.containsKey(tid) || wakingThreads.contains(tid)
  }

  override fun lock(
      lock: Any,
      lockThread: ThreadContext,
      shouldBlock: Boolean,
      lockBecauseOfWait: Boolean,
      canInterrupt: Boolean,
  ): Boolean {
    val tid = lockThread.thread.id
    if (lockHolder == null || lockHolder == tid) {
      lockHolder = tid
      if (!lockBecauseOfWait) {
        lockTimes[tid] = lockTimes.getOrDefault(tid, 0) + 1
      }
      wakingThreads.remove(tid)

      for (thread in wakingThreads.values) {
        thread.state = ThreadState.Paused
      }
      return true
    } else {
      if (canInterrupt) {
        lockThread.checkInterrupt()
      }
      if (shouldBlock) {
        lockWaiters[tid] = LockWaiter(canInterrupt, lockThread)
      }
    }
    return false
  }

  override fun unlock(
      lock: Any,
      tid: Long,
      unlockBecauseOfWait: Boolean,
      earlyExit: Boolean
  ): Boolean {
    assert(lockHolder == tid || earlyExit)
    if (lockHolder != tid && earlyExit) {
      return false
    }
    if (!unlockBecauseOfWait) {
      lockTimes[tid] = lockTimes[tid]!! - 1
    }

    if (lockTimes[tid] == 0 || unlockBecauseOfWait) {
      if (lockTimes[tid] == 0) {
        lockTimes.remove(tid)
      }
      lockHolder = null
      for (thread in wakingThreads.values) {
        if (thread.state != ThreadState.Completed) {
          thread.state = ThreadState.Enabled
        }
      }
      for (lockWaiter in lockWaiters.values) {
        if (lockWaiter.thread.state != ThreadState.Completed) {
          lockWaiter.thread.pendingOperation = ThreadResumeOperation()
          lockWaiter.thread.state = ThreadState.Enabled
        }
      }
      lockWaiters.clear()
      return true
    }
    return false
  }

  override fun interrupt(tid: Long) {
    val lockWaiter = lockWaiters[tid] ?: return
    if (lockWaiter.canInterrupt) {
      lockWaiter.thread.pendingOperation = ThreadResumeOperation()
      lockWaiter.thread.state = ThreadState.Enabled
      lockWaiters.remove(tid)
    }
  }
}
