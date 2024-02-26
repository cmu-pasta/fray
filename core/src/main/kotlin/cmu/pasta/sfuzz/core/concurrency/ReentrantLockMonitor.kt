package cmu.pasta.sfuzz.core.concurrency

import java.util.concurrent.locks.ReentrantLock

class ReentrantLockMonitor {
    val lockHolders = mutableMapOf<ReentrantLock, Long>()
    val lockWaiters = mutableMapOf<ReentrantLock, MutableSet<Long>>()

    /**
    * Return true if [lock] is acquired by the current thread.
    */
    fun lock(lock: ReentrantLock): Boolean {
        var t = Thread.currentThread()
        if (!lockHolders.contains(lock) || lockHolders[lock] == t.threadId()) {
            lockHolders[lock] = t.threadId()
            return true
        } else {
            if (!lockWaiters.contains(lock)) {
                lockWaiters[lock] = mutableSetOf()
            }
            lockWaiters[lock]?.add(t.threadId())
            return false
        }
    }

//    fun unlock(lock: ReentrantLock): Boolean {
//
//    }
}