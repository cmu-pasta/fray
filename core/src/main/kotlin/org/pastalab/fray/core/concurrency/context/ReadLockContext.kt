package org.pastalab.fray.core.concurrency.context

import java.util.concurrent.locks.Lock
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.InterruptionType
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation
import org.pastalab.fray.core.utils.Utils.verifyOrReport
import org.pastalab.fray.rmi.ThreadState

class ReadLockContext(lock: Lock) : LockContext(lock) {
  lateinit var writeLockContext: WriteLockContext

  val lockHolders = mutableSetOf<Long>()
  val lockWaiters = mutableMapOf<Long, LockWaiter>()
  val lockTimes = mutableMapOf<Long, Int>()
  override val wakingThreads = mutableMapOf<Long, ThreadContext>()
  override val signalContexts = mutableSetOf<SignalContext>()

  override fun addWakingThread(t: ThreadContext) {
    verifyOrReport(false) { "Read lock does not have waking threads" }
  }

  override fun canLock(tid: Long): Boolean {
    return canLockInternal(tid) && writeLockContext.canLockInternal(tid)
  }

  fun canLockInternal(tid: Long): Boolean = lockHolders.isEmpty() || lockHolders.contains(tid)

  override fun lock(
      lockThread: ThreadContext,
      shouldBlock: Boolean,
      lockBecauseOfWait: Boolean,
      canInterrupt: Boolean
  ): Boolean {
    verifyOrReport(!lockBecauseOfWait) { "Read lock does not have condition objects" }
    val tid = lockThread.thread.id
    if (!writeLockContext.canLockInternal(tid)) {
      if (canInterrupt) {
        lockThread.checkInterrupt()
      }
      if (shouldBlock) {
        lockWaiters[tid] = LockWaiter(canInterrupt, lockThread)
      }
      return false
    }
    lockTimes[tid] = lockTimes.getOrDefault(tid, 0) + 1
    lockHolders.add(tid)
    lockThread.acquiredResources.add(this)
    return true
  }

  override fun unlock(
      threadContext: ThreadContext,
      unlockBecauseOfWait: Boolean,
      earlyExit: Boolean
  ): Boolean {
    val tid = threadContext.thread.id
    verifyOrReport(lockHolders.contains(tid))
    verifyOrReport(!unlockBecauseOfWait) // Read lock does not have `Condition`
    lockTimes[tid] = lockTimes[tid]!! - 1
    if (lockTimes[tid] == 0) {
      lockTimes.remove(tid)
      lockHolders.remove(tid)
      if (lockHolders.isEmpty() && writeLockContext.lockHolder == null) {
        writeLockContext.unlockWaiters()
        unlockWaiters()
        threadContext.acquiredResources.remove(this)
        return true
      }
    }
    return false
  }

  fun unlockWaiters() {
    for (readLockWaiter in lockWaiters.values) {
      unblockThread(readLockWaiter.thread.thread.id, InterruptionType.RESOURCE_AVAILABLE)
    }
  }

  override fun hasQueuedThreads(): Boolean {
    return writeLockContext.hasQueuedThreadsInternal() || hasQueuedThreadsInternal()
  }

  fun hasQueuedThreadsInternal(): Boolean {
    return lockWaiters.isNotEmpty()
  }

  override fun hasQueuedThread(tid: Long): Boolean {
    return writeLockContext.hasQueuedThreadInternal(tid) || hasQueuedThreadInternal(tid)
  }

  fun hasQueuedThreadInternal(tid: Long): Boolean {
    return lockWaiters.containsKey(tid)
  }

  override fun isEmpty(): Boolean {
    return writeLockContext.isEmptyInternal() && isEmptyInternal()
  }

  fun isEmptyInternal(): Boolean {
    return lockHolders.isEmpty()
  }

  override fun isLockHolder(tid: Long): Boolean {
    return writeLockContext.isLockHolderInternal(tid) || isLockHolderInternal(tid)
  }

  fun isLockHolderInternal(tid: Long): Boolean {
    return lockHolders.contains(tid)
  }

  override fun unblockThread(tid: Long, type: InterruptionType): Boolean {
    val lockWaiter = lockWaiters[tid] ?: return false
    val noTimeout = type != InterruptionType.TIMEOUT
    if ((lockWaiter.canInterrupt && type == InterruptionType.INTERRUPT) ||
        (type == InterruptionType.FORCE) ||
        (type == InterruptionType.RESOURCE_AVAILABLE) ||
        (type == InterruptionType.TIMEOUT)) {
      lockWaiter.thread.pendingOperation = ThreadResumeOperation(noTimeout)
      lockWaiter.thread.state = ThreadState.Runnable
      lockWaiters.remove(tid)
    }
    return false
  }

  override fun getNumThreadsWaitingForLockDueToSignal(): Int {
    return 0
  }
}
