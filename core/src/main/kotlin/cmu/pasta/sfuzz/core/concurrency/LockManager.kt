package cmu.pasta.sfuzz.core.concurrency

import cmu.pasta.sfuzz.core.ThreadContext
import cmu.pasta.sfuzz.core.ThreadState
import cmu.pasta.sfuzz.core.concurrency.operations.ThreadResumeOperation
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock

class LockManager {
    val lockContextManager = LockContextManager()

    val waitingThreads = mutableMapOf<Int, MutableList<Long>>()

    val conditionToLock = mutableMapOf<Condition, Lock>()
    val lockToConditions = mutableMapOf<Lock, MutableList<Condition>>()

    fun getLockContext(lock: Any): ReentrantLockContext {
        return lockContextManager.getLockContext(lock)
    }

    /**
    * Return true if [lock] is acquired by the current thread.
    */
    fun lock(lock: Any, tid: Long, shouldBlock: Boolean, lockBecauseOfWait: Boolean): Boolean {
        return getLockContext(lock).lock(lock, tid, shouldBlock, lockBecauseOfWait)
    }

    fun addWakingThread(lockObject: Any, t: Thread) {
        getLockContext(lockObject).addWakingThread(lockObject, t)
    }

    fun addWaitingThread(waitingObject: Any, t: Thread) {
        val id = System.identityHashCode(waitingObject)
        if (id !in waitingThreads) {
            waitingThreads[id] = mutableListOf()
        }
        waitingThreads[id]!!.add(t.id)
    }

    // TODO(aoli): can we merge this logic with `objectNotifyImply`?
    fun threadInterruptDuringObjectWait(waitingObject: Any, lockObject: Any, context: ThreadContext) {
        val id = System.identityHashCode(waitingObject)
        val lockContext = getLockContext(lockObject)
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
        conditionToLock.clear()
        lockToConditions.clear()
//        lockContextManager.done()
    }
}