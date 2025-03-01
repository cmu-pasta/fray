package org.pastalab.fray.core.concurrency.primitives

import java.util.concurrent.locks.StampedLock
import java.util.concurrent.locks.StampedLock.isOptimisticReadStamp
import java.util.concurrent.locks.StampedLock.isReadLockStamp
import java.util.concurrent.locks.StampedLock.isWriteLockStamp
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.InterruptionType
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation
import org.pastalab.fray.rmi.ResourceInfo
import org.pastalab.fray.rmi.ResourceType
import org.pastalab.fray.rmi.ThreadState

class StampedLockContext(stampedLock: StampedLock) :
    InterruptibleContext,
    Acquirable(ResourceInfo(System.identityHashCode(stampedLock), ResourceType.LOCK)) {
  var readHolders = 0
  var writeLockAcquired = false
  val readLockWaiters = mutableMapOf<Long, LockWaiter>()
  val writeLockWaiters = mutableMapOf<Long, LockWaiter>()

  fun readLock(lockThread: ThreadContext, shouldBlock: Boolean, canInterrupt: Boolean): Boolean {
    if (writeLockAcquired) {
      if (canInterrupt) {
        lockThread.checkInterrupt()
      }
      if (shouldBlock) {
        readLockWaiters[lockThread.thread.id] = LockWaiter(canInterrupt, lockThread)
      }
      return false
    }
    readHolders += 1
    lockThread.acquiredResources.add(this)
    return true
  }

  fun unblockAllWaiters() {
    readLockWaiters.values.forEach {
      it.thread.pendingOperation = ThreadResumeOperation(true)
      it.thread.state = ThreadState.Runnable
      readLockWaiters.remove(it.thread.thread.id)
    }
    writeLockWaiters.values.forEach {
      it.thread.pendingOperation = ThreadResumeOperation(true)
      it.thread.state = ThreadState.Runnable
      writeLockWaiters.remove(it.thread.thread.id)
    }
  }

  fun unlockReadLock(threadContext: ThreadContext) {
    readHolders -= 1
    threadContext.acquiredResources.remove(this)
    if (readHolders == 0) {
      unblockAllWaiters()
    }
  }

  fun unlockWriteLock(threadContext: ThreadContext) {
    writeLockAcquired = false
    threadContext.acquiredResources.remove(this)
    unblockAllWaiters()
  }

  fun writeLock(lockThread: ThreadContext, shouldBlock: Boolean, canInterrupt: Boolean): Boolean {
    if (readHolders == 0 && !writeLockAcquired) {
      writeLockAcquired = true
      lockThread.acquiredResources.add(this)
      return true
    }
    if (canInterrupt) {
      lockThread.checkInterrupt()
    }
    if (shouldBlock) {
      writeLockWaiters[lockThread.thread.id] = LockWaiter(canInterrupt, lockThread)
    }
    return false
  }

  fun convertToReadLock(stamp: Long) {
    if (isWriteLockStamp(stamp)) {
      writeLockAcquired = false
      readHolders += 1
    }
    if (isOptimisticReadStamp(stamp)) {
      readHolders += 1
    }
  }

  fun convertToWriteLock(stamp: Long) {
    if (isReadLockStamp(stamp)) {
      readHolders -= 1
    }
    writeLockAcquired = true
  }

  fun convertToOptimisticReadLock(stamp: Long) {
    if (isReadLockStamp(stamp)) {
      readHolders -= 1
    }
    if (isWriteLockStamp(stamp)) {
      writeLockAcquired = false
    }
  }

  override fun unblockThread(tid: Long, type: InterruptionType): Boolean {
    val noTimeout = type != InterruptionType.TIMEOUT
    readLockWaiters[tid]?.let {
      if (it.canInterrupt) {
        it.thread.pendingOperation = ThreadResumeOperation(noTimeout)
        it.thread.state = ThreadState.Runnable
        readLockWaiters.remove(tid)
      }
    }
    writeLockWaiters[tid]?.let {
      if (it.canInterrupt) {
        it.thread.pendingOperation = ThreadResumeOperation(noTimeout)
        it.thread.state = ThreadState.Runnable
        writeLockWaiters.remove(tid)
      }
    }
    return false
  }
}
