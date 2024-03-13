package cmu.pasta.sfuzz.core.concurrency

import cmu.pasta.sfuzz.core.ThreadState
import cmu.pasta.sfuzz.core.GlobalContext
import cmu.pasta.sfuzz.core.concurrency.operations.ThreadResumeOperation

class ReentrantLockMonitor {
    val lockHolders = mutableMapOf<Int, Long>()
    val lockWaiters = mutableMapOf<Int, MutableList<Long>>()

    // This map stores all threads that is woken by `object.Notify`
    // but not yet acquired the monitor lock.
    val threadWaiters = mutableMapOf<Int, MutableList<Long>>()

    /**
    * Return true if [lock] is acquired by the current thread.
    */
    fun lock(lock: Any, tid: Long, shouldBlock: Boolean): Boolean {
        val id = System.identityHashCode(lock)
        if (!lockHolders.contains(id) || lockHolders[id] == tid) {
            lockHolders[id] = tid
            threadWaiters[id]?.let {
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

    fun addWaiter(lock: Any, t: Thread) {
        val id = System.identityHashCode(lock)
        if (!threadWaiters.contains(id)) {
            threadWaiters[id] = mutableListOf()
        }
        threadWaiters[id]!!.add(t.id)
    }

    fun unlock(lock: Any): Int {
        val id = System.identityHashCode(lock)
        assert(lockHolders[id] == Thread.currentThread().id)
        lockHolders.remove(id)
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
        threadWaiters[id]?.let {
            for (thread in it) {
                GlobalContext.registeredThreads[thread]!!.state = ThreadState.Enabled
            }
            return it.size
        }
        return 0
    }

    fun done() {
        assert(lockHolders.isEmpty())
        assert(lockWaiters.isEmpty())
    }
}