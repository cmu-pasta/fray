package cmu.pasta.sfuzz.core.concurrency

import cmu.pasta.sfuzz.core.GlobalContext
import cmu.pasta.sfuzz.core.ThreadState
import cmu.pasta.sfuzz.core.concurrency.operations.ThreadResumeOperation

class ReentrantLockMonitor {
    val lockHolders = mutableMapOf<Int, Long>()
    // LockReentrantMap maps from thread id to lock id
    // to the number of times the lock is acquired.
    val lockReentrantMap = mutableMapOf<Long, MutableMap<Int, Int>>()
    val lockWaiters = mutableMapOf<Int, MutableList<Long>>()

    // This map stores all threads that is woken by `object.Notify`
    // but not yet acquired the monitor lock.
    val wakingThreads = mutableMapOf<Int, MutableSet<Long>>()
    val waitingThreads = mutableMapOf<Int, MutableList<Long>>()

    /**
    * Return true if [lock] is acquired by the current thread.
    */
    fun lock(lock: Any, tid: Long, shouldBlock: Boolean, lockBecauseOfWait: Boolean): Boolean {
        val id = System.identityHashCode(lock)
        if (!lockHolders.contains(id) || lockHolders[id] == tid) {
            lockHolders[id] = tid
            if (!lockReentrantMap.contains(tid)) {
                lockReentrantMap[tid] = mutableMapOf()
            }
            if (!lockBecauseOfWait) {
                lockReentrantMap[tid]!![id] = lockReentrantMap[tid]!!.getOrDefault(id, 0) + 1
            }
            wakingThreads[id]?.let {
                it.remove(tid)
                for (thread in it) {
                    GlobalContext.registeredThreads[thread]!!.state = ThreadState.Paused
                }
            }
            return true
        } else if (shouldBlock) {
            if (!lockWaiters.contains(id)) {
                lockWaiters[id] = mutableListOf()
            }
            lockWaiters[id]?.add(tid)
        }
        return false
    }

    fun addWakingThread(lock: Any, t: Thread) {
        val id = System.identityHashCode(lock)
        if (!wakingThreads.contains(id)) {
            wakingThreads[id] = mutableSetOf()
        }
        wakingThreads[id]!!.add(t.id)
    }

    fun addWaitingThread(lock: Any, t: Thread) {
        val id = System.identityHashCode(lock)
        if (id !in waitingThreads) {
            waitingThreads[id] = mutableListOf()
        }
        waitingThreads[id]!!.add(t.id)
    }

    fun getNumThreadsBlockBy(lock: Any): Int {
        val id = System.identityHashCode(lock)
        return (wakingThreads[id]?.size ?: 0) + (waitingThreads[id]?.size ?: 0)
    }

    fun getNumThreadsBlockBy(lock: Any, condition:Any): Int {
        val id = System.identityHashCode(lock)
        val conditionId = System.identityHashCode(condition)
        return (wakingThreads[id]?.size ?: 0) + (waitingThreads[condition]?.size ?: 0)
    }

    fun unlock(lock: Any, tid: Long, unlockBecauseOfWait: Boolean): Boolean {
        val id = System.identityHashCode(lock)
        assert(lockHolders[id] == Thread.currentThread().id)
        if (!unlockBecauseOfWait) {
            lockReentrantMap[tid]!![id] = lockReentrantMap[tid]!![id]!! - 1
        }
        if (lockReentrantMap[tid]!![id] == 0 || unlockBecauseOfWait) {
            lockHolders.remove(id)
            if (lockReentrantMap[tid]!![id] == 0) {
                lockReentrantMap[tid]!!.remove(id)
                if (lockReentrantMap[tid]!!.isEmpty()) {
                    lockReentrantMap.remove(tid)
                }
            }
            // TODO(aoli): Does this give higher priority to threadWaiters?
            lockWaiters[id]?.let {
                if (it.size > 0) {
                    var result = it.removeFirst()
                    GlobalContext.registeredThreads[result]?.pendingOperation = ThreadResumeOperation()
                    GlobalContext.registeredThreads[result]?.state = ThreadState.Enabled
                }
                if (it.size == 0) {
                    lockWaiters.remove(id)
                }
            }
            wakingThreads[id]?.let {
                for (thread in it) {
                    GlobalContext.registeredThreads[thread]!!.state = ThreadState.Enabled
                }
            }
            return true
        }
        return false
    }

    fun done() {
        assert(lockHolders.isEmpty())
        assert(lockWaiters.isEmpty())
    }
}