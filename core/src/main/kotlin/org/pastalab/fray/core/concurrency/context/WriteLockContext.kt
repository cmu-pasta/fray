package org.pastalab.fray.core.concurrency.context

import java.util.concurrent.locks.Lock
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.InterruptionType
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation
import org.pastalab.fray.core.utils.Utils.verifyOrReport
import org.pastalab.fray.rmi.ThreadState

class WriteLockContext(lock: Lock) : LockContext(lock) {
  lateinit var readLockContext: ReadLockContext
  private val lockWaiters = mutableMapOf<Long, LockWaiter>()
  private val lockTimes = mutableMapOf<Long, Int>()
  override val signalContexts = mutableSetOf<SignalContext>()
  override val wakingThreads = mutableMapOf<Long, ThreadContext>()
  var lockHolder: Long? = null

  override fun addWakingThread(t: ThreadContext) {
    wakingThreads[t.thread.id] = t
  }

  override fun canLock(tid: Long): Boolean {
    return canLockInternal(tid) && readLockContext.canLockInternal(tid)
  }

  fun canLockInternal(tid: Long): Boolean = lockHolder == null || lockHolder == tid

  override fun lock(
      lockThread: ThreadContext,
      shouldBlock: Boolean,
      lockBecauseOfWait: Boolean,
      canInterrupt: Boolean
  ): Boolean {
    if (!canLock(lockThread.thread.id)) {
      if (canInterrupt) {
        lockThread.checkInterrupt()
      }
      if (shouldBlock) {
        lockWaiters[lockThread.thread.id] = LockWaiter(canInterrupt, lockThread)
      }
      return false
    }
    if (!lockBecauseOfWait) {
      lockTimes[lockThread.thread.id] = lockTimes.getOrDefault(lockThread.thread.id, 0) + 1
    }
    lockHolder = lockThread.thread.id
    lockThread.acquiredResources.add(this)
    return true
  }

  override fun unlock(
      lockThread: ThreadContext,
      unlockBecauseOfWait: Boolean,
      earlyExit: Boolean
  ): Boolean {
    val tid = lockThread.thread.id
    verifyOrReport(lockHolder == tid) { "Thread $tid is not the lock holder" }
    if (!unlockBecauseOfWait) {
      lockTimes[tid] = lockTimes[tid]!! - 1
    }
    if (lockTimes[tid] == 0 || unlockBecauseOfWait) {
      if (lockTimes[tid] == 0) {
        lockTimes.remove(tid)
      }
      lockHolder = null
      readLockContext.unlockWaiters()
      if (readLockContext.lockHolders.isEmpty()) {
        unlockWaiters()
        lockThread.acquiredResources.add(this)
        return true
      }
    }
    return false
  }

  override fun hasQueuedThreads(): Boolean {
    return hasQueuedThreadsInternal() || readLockContext.hasQueuedThreadsInternal()
  }

  fun hasQueuedThreadsInternal(): Boolean {
    return lockWaiters.any() || wakingThreads.any()
  }

  override fun hasQueuedThread(tid: Long): Boolean {
    return hasQueuedThreadInternal(tid) || readLockContext.hasQueuedThreadInternal(tid)
  }

  fun hasQueuedThreadInternal(tid: Long): Boolean {
    return lockWaiters.containsKey(tid) || wakingThreads.contains(tid)
  }

  override fun isEmpty(): Boolean {
    return isEmptyInternal() && readLockContext.isEmptyInternal()
  }

  fun isEmptyInternal(): Boolean {
    return lockHolder == null &&
        lockTimes.isEmpty() &&
        lockWaiters.isEmpty() &&
        wakingThreads.isEmpty()
  }

  override fun isLockHolder(tid: Long): Boolean {
    return isLockHolderInternal(tid) || readLockContext.isLockHolderInternal(tid)
  }

  fun isLockHolderInternal(tid: Long): Boolean {
    return lockHolder == tid
  }

  override fun unblockThread(tid: Long, type: InterruptionType): Boolean {
    val lockWaiter = lockWaiters[tid] ?: return false
    val noTimeout = type != InterruptionType.TIMEOUT
    if ((lockWaiter.canInterrupt && type == InterruptionType.INTERRUPT) ||
        (type == InterruptionType.RESOURCE_AVAILABLE) ||
        (type == InterruptionType.TIMEOUT) ||
        (type == InterruptionType.FORCE)) {
      lockWaiter.thread.pendingOperation = ThreadResumeOperation(noTimeout)
      lockWaiter.thread.state = ThreadState.Runnable
      lockWaiters.remove(tid)
    }
    return false
  }

  fun unlockWaiters() {
    for (threadId in lockWaiters.values.map { it.thread.thread.id }.toList()) {
      unblockThread(threadId, InterruptionType.RESOURCE_AVAILABLE)
    }
    // Waking threads are write waiters as well.
    for (thread in wakingThreads.values) {
      thread.pendingOperation = ThreadResumeOperation(true)
      thread.state = ThreadState.Runnable
    }
  }

  override fun getNumThreadsWaitingForLockDueToSignal(): Int {
    return signalContexts.sumOf { it.waitingThreads.size } + wakingThreads.size
  }
}
