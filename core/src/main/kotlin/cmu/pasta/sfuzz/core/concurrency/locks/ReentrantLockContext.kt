package cmu.pasta.sfuzz.core.concurrency.locks

import cmu.pasta.sfuzz.core.GlobalContext
import cmu.pasta.sfuzz.core.ThreadState
import cmu.pasta.sfuzz.core.concurrency.operations.ThreadResumeOperation

class ReentrantLockContext: LockContext {
    private var lockHolder: Long? = null
    private val lockTimes = mutableMapOf<Long, Int>()
    private val lockWaiters: MutableList<Long> = mutableListOf()
    override val wakingThreads: MutableSet<Long> = mutableSetOf()

    override fun addWakingThread(lockObject: Any, t: Thread) {
        wakingThreads.add(t.id)
    }
    override fun canLock(tid: Long) = lockHolder == null || lockHolder == tid

    override fun isEmpty(): Boolean = lockHolder == null
            && lockTimes.isEmpty()
            && lockWaiters.isEmpty()
            && wakingThreads.isEmpty()

    override fun lock(lock: Any, tid: Long, shouldBlock: Boolean, lockBecauseOfWait: Boolean): Boolean {
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
        } else if (shouldBlock) {
            lockWaiters.add(tid)
        }
        return false
    }

    override fun unlock(lock: Any, tid: Long, unlockBecauseOfWait: Boolean): Boolean {
        assert(lockHolder == tid)
        if (!unlockBecauseOfWait) {
            lockTimes[tid] = lockTimes[tid]!! - 1
        }

        if (lockTimes[tid] == 0 || unlockBecauseOfWait) {
            if (lockTimes[tid] == 0) {
                lockTimes.remove(tid)
            }
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