package cmu.pasta.fray.core.concurrency.locks

import cmu.pasta.fray.core.GlobalContext
import cmu.pasta.fray.core.ThreadState
import cmu.pasta.fray.core.concurrency.operations.ThreadResumeOperation

class ReentrantLockContext : LockContext {
  private var lockHolder: Long? = null
  private val lockTimes = mutableMapOf<Long, Int>()
  // Mapping from thread id to whether the thread is interruptible.
  private val lockWaiters = mutableMapOf<Long, Boolean>()
  override val wakingThreads: MutableSet<Long> = mutableSetOf()

  override fun addWakingThread(lockObject: Any, t: Thread) {
    wakingThreads.add(t.id)
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
      tid: Long,
      shouldBlock: Boolean,
      lockBecauseOfWait: Boolean,
      canInterrupt: Boolean,
  ): Boolean {
    if (lockHolder == null || lockHolder == tid) {
      lockHolder = tid
      if (!lockBecauseOfWait) {
        lockTimes[tid] = lockTimes.getOrDefault(tid, 0) + 1
      }
      wakingThreads.remove(tid)

      // TODO(aoli): I don't like the design that we need to access GlobalContext here.
      for (thread in wakingThreads) {
        GlobalContext.registeredThreads[thread]!!.state = ThreadState.Paused
      }
      return true
    } else {
      if (canInterrupt) {
        GlobalContext.registeredThreads[tid]?.checkInterrupt()
      }
      if (shouldBlock) {
        lockWaiters[tid] = canInterrupt
      }
    }
    return false
  }

  override fun unlock(lock: Any, tid: Long, unlockBecauseOfWait: Boolean): Boolean {
    assert(lockHolder == tid || GlobalContext.bugFound != null)
    if (lockHolder != tid && GlobalContext.bugFound != null) {
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
      for (thread in wakingThreads) {
        val context = GlobalContext.registeredThreads[thread]!!
        if (context.state != ThreadState.Completed) {
          context.state = ThreadState.Enabled
        }
      }
      for (thread in lockWaiters.keys) {
        val context = GlobalContext.registeredThreads[thread]!!
        if (context.state != ThreadState.Completed) {
          context.pendingOperation = ThreadResumeOperation()
          context.state = ThreadState.Enabled
        }
      }
      lockWaiters.clear()
      return true
    }
    return false
  }

  override fun interrupt(tid: Long) {
    if (lockWaiters[tid] == true) {
      GlobalContext.registeredThreads[tid]!!.pendingOperation = ThreadResumeOperation()
      GlobalContext.registeredThreads[tid]!!.state = ThreadState.Enabled
      lockWaiters.remove(tid)
    }
  }
}
