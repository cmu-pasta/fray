package cmu.pasta.fray.core.concurrency.locks

import cmu.pasta.fray.core.ThreadContext
import cmu.pasta.fray.core.ThreadState
import cmu.pasta.fray.core.concurrency.operations.ThreadResumeOperation
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock

class LockManager {
  val lockContextManager = ReferencedContextManager<LockContext> { ReentrantLockContext() }
  val waitingThreads = mutableMapOf<Int, MutableList<Long>>()
  val threadWaitsFor = mutableMapOf<Long, Int>()
  val conditionToLock = mutableMapOf<Condition, Lock>()
  val lockToConditions = mutableMapOf<Lock, MutableList<Condition>>()

  fun threadUnblockedDueToDeadlock(t: Thread) {
    val id = threadWaitsFor[t.id] ?: return
    waitingThreads[id]?.remove(t.id)
    if (waitingThreads[id]?.isEmpty() == true) {
      waitingThreads.remove(id)
    }
    threadWaitsFor.remove(t.id)
  }

  fun getLockContext(lock: Any): LockContext {
    return lockContextManager.getLockContext(lock)
  }

  fun reentrantReadWriteLockInit(readLock: ReadLock, writeLock: WriteLock) {
    val context = ReentrantReadWriteLockContext()
    lockContextManager.addContext(readLock, context)
    lockContextManager.addContext(writeLock, context)
  }

  /** Return true if [lock] is acquired by the current thread. */
  fun lock(
      lock: Any,
      tid: Long,
      shouldBlock: Boolean,
      lockBecauseOfWait: Boolean,
      canInterrupt: Boolean
  ): Boolean {
    return getLockContext(lock).lock(lock, tid, shouldBlock, lockBecauseOfWait, canInterrupt)
  }

  fun addWakingThread(lockObject: Any, t: Thread) {
    getLockContext(lockObject).addWakingThread(lockObject, t)
  }

  fun addWaitingThread(waitingObject: Any, t: Thread) {
    val id = System.identityHashCode(waitingObject)
    if (id !in waitingThreads) {
      waitingThreads[id] = mutableListOf()
    }
    assert(t.id !in waitingThreads[id]!!)
    waitingThreads[id]!!.add(t.id)
    threadWaitsFor[t.id] = id
  }

  // TODO(aoli): can we merge this logic with `objectNotifyImply`?
  fun threadInterruptDuringObjectWait(waitingObject: Any, lockObject: Any, context: ThreadContext) {
    val id = System.identityHashCode(waitingObject)
    val lockContext = getLockContext(lockObject)
    threadWaitsFor.remove(context.thread.id)
    waitingThreads[id]?.remove(context.thread.id)
    if (waitingThreads[id]?.isEmpty() == true) {
      waitingThreads.remove(id)
    }
    addWakingThread(lockObject, context.thread)
    if (lockContext.canLock(context.thread.id)) {
      context.pendingOperation = ThreadResumeOperation()
      context.state = ThreadState.Enabled
    }
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

  fun getNumThreadsBlockBy(lock: Any): Int {
    val id = System.identityHashCode(lock)
    return (getLockContext(lock).wakingThreads.size) + (waitingThreads[id]?.size ?: 0)
  }

  fun unlock(lock: Any, tid: Long, unlockBecauseOfWait: Boolean): Boolean {
    return getLockContext(lock).unlock(lock, tid, unlockBecauseOfWait)
  }

  fun done() {
    assert(waitingThreads.isEmpty())
    assert(threadWaitsFor.isEmpty())
    conditionToLock.clear()
    lockToConditions.clear()
    lockContextManager.done()
  }
}