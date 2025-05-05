package org.pastalab.fray.core.concurrency.context

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.InterruptionType
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation
import org.pastalab.fray.core.utils.Utils.verifyOrReport
import org.pastalab.fray.rmi.ThreadState

class ReentrantLockContext(lock: Any) : LockContext(lock) {
  var lockHolder: Long? = null
  private val lockTimes = mutableMapOf<Long, Int>()
  // Mapping from thread id to whether the thread is interruptible.
  private val lockWaiters = mutableMapOf<Long, LockWaiter>()
  override val wakingThreads = mutableMapOf<Long, ThreadContext>()
  override val signalContexts = mutableSetOf<SignalContext>()

  override fun addWakingThread(t: ThreadContext) {
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
        thread.state = ThreadState.Blocked
      }
      lockThread.acquiredResources.add(this)
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
      threadContext: ThreadContext,
      unlockBecauseOfWait: Boolean,
      earlyExit: Boolean
  ): Boolean {
    val tid = threadContext.thread.id
    verifyOrReport(lockHolder == tid || earlyExit)
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
          thread.state = ThreadState.Runnable
        }
      }
      for (lockWaiter in lockWaiters.values) {
        if (lockWaiter.thread.state != ThreadState.Completed) {
          lockWaiter.thread.pendingOperation = ThreadResumeOperation(true)
          lockWaiter.thread.state = ThreadState.Runnable
        }
      }
      lockWaiters.clear()
      threadContext.acquiredResources.remove(this)
      return true
    }
    return false
  }

  override fun isLockHolder(tid: Long): Boolean {
    return lockHolder == tid
  }

  override fun unblockThread(tid: Long, type: InterruptionType): Boolean {
    val lockWaiter = lockWaiters[tid] ?: return false
    if ((lockWaiter.canInterrupt && type == InterruptionType.INTERRUPT) ||
        (type == InterruptionType.FORCE) ||
        (type == InterruptionType.TIMEOUT)) {
      lockWaiter.thread.pendingOperation = ThreadResumeOperation(type != InterruptionType.TIMEOUT)
      lockWaiter.thread.state = ThreadState.Runnable
      lockWaiters.remove(tid)
    }
    return false
  }

  override fun getNumThreadsWaitingForLockDueToSignal(): Int {
    return signalContexts.sumOf { it.waitingThreads.size } + wakingThreads.size
  }
}
