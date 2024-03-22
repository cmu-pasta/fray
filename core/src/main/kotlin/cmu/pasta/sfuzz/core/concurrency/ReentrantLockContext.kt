package cmu.pasta.sfuzz.core.concurrency

import cmu.pasta.sfuzz.core.GlobalContext
import cmu.pasta.sfuzz.core.ThreadState
import cmu.pasta.sfuzz.core.concurrency.operations.ThreadResumeOperation

class ReentrantLockContext: LockContext {
    private var lockHolder: Long? = null
    private var lockTimes = 0
    private val lockTimesCache = mutableMapOf<Long, Int>()
    private val lockWaiters: MutableList<Long> = mutableListOf()
    val wakingThreads: MutableSet<Long> = mutableSetOf()

    fun addWakingThread(lockObject: Any, t: Thread) {
        wakingThreads.add(t.id)
    }
    fun canLock(tid: Long) = lockHolder == null || lockHolder == tid

    fun lock(lock: Any, tid: Long, shouldBlock: Boolean, lockBecauseOfWait: Boolean): Boolean {
        if (lockHolder == null || lockHolder == tid) {
            lockHolder = tid
            if (lockBecauseOfWait) {
                lockTimes = lockTimesCache[tid]!!
                lockTimesCache.remove(tid)
            } else {
                lockTimes += 1
            }
            wakingThreads.remove(tid)

            // TODO(aoli): I don't like the design that we need to access GlobalContext here.
            for (thread in wakingThreads) {
                GlobalContext.registeredThreads[thread]!!.state = ThreadState.Paused
            }
            return true
        } else if (shouldBlock) {
            lockWaiters.add(tid)
        }
        return false
    }

    fun unlock(lock: Any, tid: Long, unlockBecauseOfWait: Boolean): Boolean {
        assert(lockHolder == tid)
        if (!unlockBecauseOfWait) {
            lockTimes -= 1
        } else {
            lockTimesCache[tid] = lockTimes
            lockTimes = 0
        }

        if (lockTimes == 0) {
            lockHolder = null
            for (thread in wakingThreads) {
                GlobalContext.registeredThreads[thread]!!.pendingOperation = ThreadResumeOperation()
                GlobalContext.registeredThreads[thread]!!.state = ThreadState.Enabled
            }
            for (thread in lockWaiters) {
                GlobalContext.registeredThreads[thread]!!.pendingOperation = ThreadResumeOperation()
                GlobalContext.registeredThreads[thread]!!.state = ThreadState.Enabled
            }
            lockWaiters.clear()
            return true
        }
        return false
    }
}