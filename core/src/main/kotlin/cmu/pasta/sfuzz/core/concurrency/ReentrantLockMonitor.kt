package cmu.pasta.sfuzz.core.concurrency

import cmu.pasta.sfuzz.core.ThreadState
import cmu.pasta.sfuzz.core.GlobalContext
import cmu.pasta.sfuzz.core.concurrency.operations.ThreadResumeOperation

class ReentrantLockMonitor {
    val lockHolders = mutableMapOf<Int, Long>()
    val lockWaiters = mutableMapOf<Int, MutableList<Long>>()

    /**
    * Return true if [lock] is acquired by the current thread.
    */
    fun lock(lock: Any, shouldBlock: Boolean): Boolean {
        val t = Thread.currentThread()
        val id = System.identityHashCode(lock)
        if (!lockHolders.contains(id) || lockHolders[id] == t.id) {
            lockHolders[id] = t.id
            return true
        } else if (shouldBlock) {
            if (!lockWaiters.contains(id)) {
                lockWaiters[id] = mutableListOf()
            }
            lockWaiters[id]?.add(t.id)
        }
        return false
    }

    /**
     * Return true if [lock] is acquired by the current thread.
     */
    fun tryLock(lock: Any) {
        val t = Thread.currentThread()
        val id = System.identityHashCode(lock)
        if (!lockHolders.contains(id) || lockHolders[id] == t.id) {
            lockHolders[id] = t.id
        }
    }

    fun unlock(lock: Any) {
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
    }

    fun done() {
        assert(lockHolders.isEmpty())
        assert(lockWaiters.isEmpty())
    }
}