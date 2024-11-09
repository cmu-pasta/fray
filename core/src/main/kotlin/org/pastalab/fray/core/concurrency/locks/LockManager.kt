package org.pastalab.fray.core.concurrency.locks

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.ThreadState
import org.pastalab.fray.core.concurrency.operations.ConditionAwaitBlocked
import org.pastalab.fray.core.concurrency.operations.ConditionWakeBlocked
import org.pastalab.fray.core.concurrency.operations.ObjectWakeBlocked
import org.pastalab.fray.core.utils.Utils.verifyOrReport

class LockManager {
  val lockContextManager = ReferencedContextManager<LockContext> { ReentrantLockContext() }
  val waitingThreads = mutableMapOf<Int, MutableList<Long>>()
  val threadWaitsFor = mutableMapOf<Long, Int>()
  val conditionToLock = mutableMapOf<Condition, Lock>()
  val lockToConditions = mutableMapOf<Lock, MutableList<Condition>>()

  fun threadUnblockedDueToDeadlock(t: Thread) {
    val id = threadWaitsFor[t.id]
    waitingThreads[id]?.remove(t.id)
    if (waitingThreads[id]?.isEmpty() == true) {
      waitingThreads.remove(id)
    }
    threadWaitsFor.remove(t.id)
    //    val pendingOperation = registerNewCondition
  }

  fun getLockContext(lock: Any): LockContext {
    return lockContextManager.getContext(lock)
  }

  fun reentrantReadWriteLockInit(readLock: ReadLock, writeLock: WriteLock) {
    val context = ReentrantReadWriteLockContext()
    lockContextManager.addContext(readLock, context)
    lockContextManager.addContext(writeLock, context)
  }

  /** Return true if [lock] is acquired by the current thread. */
  fun lock(
      lock: Any,
      lockThread: ThreadContext,
      shouldBlock: Boolean,
      lockBecauseOfWait: Boolean,
      canInterrupt: Boolean
  ): Boolean {
    return getLockContext(lock).lock(lock, lockThread, shouldBlock, lockBecauseOfWait, canInterrupt)
  }

  fun hasQueuedThreads(lock: Any): Boolean {
    return getLockContext(lock).hasQueuedThreads()
  }

  fun hasQueuedThread(lock: Any, t: Thread): Boolean {
    return getLockContext(lock).hasQueuedThread(t.id)
  }

  fun addWakingThread(lockObject: Any, t: ThreadContext) {
    getLockContext(lockObject).addWakingThread(lockObject, t)
  }

  fun addWaitingThread(waitingObject: Any, t: Thread) {
    val id = System.identityHashCode(waitingObject)
    if (id !in waitingThreads) {
      waitingThreads[id] = mutableListOf()
    }
    verifyOrReport(t.id !in waitingThreads[id]!!)
    waitingThreads[id]!!.add(t.id)
    threadWaitsFor[t.id] = id
  }

  fun tryLockUnblocked(lock: Any, tid: Long) {
    getLockContext(lock).tryLockUnblocked(lock, tid)
  }

  // TODO(aoli): can we merge this logic with `objectNotifyImply`?
  fun objectWaitUnblockedWithoutNotify(
      waitingObject: Any,
      lockObject: Any,
      context: ThreadContext,
      isTimeout: Boolean
  ): Boolean {
    val id = System.identityHashCode(waitingObject)
    val lockContext = getLockContext(lockObject)
    threadWaitsFor.remove(context.thread.id)
    waitingThreads[id]?.remove(context.thread.id)
    if (waitingThreads[id]?.isEmpty() == true) {
      waitingThreads.remove(id)
    }
    addWakingThread(lockObject, context)
    if (waitingObject == lockObject) {
      context.pendingOperation = ObjectWakeBlocked(waitingObject, !isTimeout)
    } else {
      context.pendingOperation =
          ConditionWakeBlocked(
              waitingObject as Condition,
              (context.pendingOperation as ConditionAwaitBlocked).canInterrupt,
              !isTimeout)
    }
    if (lockContext.canLock(context.thread.id)) {
      context.state = ThreadState.Enabled
      return true
    }
    return false
  }

  fun lockFromCondition(condition: Condition): Lock {
    return conditionToLock[condition]!!
  }

  fun conditionFromLock(lock: Lock): MutableList<Condition> {
    return lockToConditions[lock]!!
  }

  fun registerNewCondition(condition: Condition, lock: Lock) {
    conditionToLock[condition] = lock
    if (lock !in lockToConditions) {
      lockToConditions[lock] = mutableListOf()
    }
    lockToConditions[lock]!!.add(condition)
  }

  fun getNumThreadsBlockBy(lock: Any, isMonitorLock: Boolean): Int {
    val id = System.identityHashCode(lock)
    return (getLockContext(lock).wakingThreads.size) +
        if (isMonitorLock) {
          (waitingThreads[id]?.size ?: 0)
        } else {
          lockToConditions[lock]?.sumOf { condition ->
            waitingThreads[System.identityHashCode(condition)]?.size ?: 0
          } ?: 0
        }
  }

  fun unlock(lock: Any, tid: Long, unlockBecauseOfWait: Boolean, earlyExit: Boolean): Boolean {
    return getLockContext(lock).unlock(lock, tid, unlockBecauseOfWait, earlyExit)
  }

  fun done() {
    verifyOrReport(waitingThreads.isEmpty())
    verifyOrReport(threadWaitsFor.isEmpty())
    conditionToLock.clear()
    lockToConditions.clear()
    lockContextManager.done()
  }
}
