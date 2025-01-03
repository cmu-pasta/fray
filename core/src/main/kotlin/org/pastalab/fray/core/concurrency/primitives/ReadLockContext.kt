package org.pastalab.fray.core.concurrency.primitives

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation
import org.pastalab.fray.core.utils.Utils.verifyOrReport
import org.pastalab.fray.rmi.ThreadState

class ReadLockContext : LockContext {
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
    return true
  }

  override fun unlock(tid: Long, unlockBecauseOfWait: Boolean, earlyExit: Boolean): Boolean {
    verifyOrReport(lockHolders.contains(tid))
    verifyOrReport(!unlockBecauseOfWait) // Read lock does not have `Condition`
    lockTimes[tid] = lockTimes[tid]!! - 1
    if (lockTimes[tid] == 0) {
      lockTimes.remove(tid)
      lockHolders.remove(tid)
      if (lockHolders.isEmpty() && writeLockContext.lockHolder == null) {
        writeLockContext.unlockWaiters()
        unlockWaiters()
        return true
      }
    }
    return false
  }

  fun unlockWaiters() {
    for (readLockWaiter in lockWaiters.values) {
      readLockWaiter.thread.pendingOperation = ThreadResumeOperation(true)
      readLockWaiter.thread.state = ThreadState.Enabled
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
        (type == InterruptionType.TIMEOUT)) {
      lockWaiter.thread.pendingOperation = ThreadResumeOperation(noTimeout)
      lockWaiter.thread.state = ThreadState.Enabled
      lockWaiters.remove(tid)
    }
    return false
  }

  override fun getNumThreadsWaitingForLockDueToSignal(): Int {
    return 0
  }
}
