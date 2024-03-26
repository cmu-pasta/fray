package cmu.pasta.sfuzz.core.concurrency.locks

import cmu.pasta.sfuzz.core.GlobalContext
import cmu.pasta.sfuzz.core.ThreadState
import cmu.pasta.sfuzz.core.concurrency.operations.ThreadResumeOperation

class CountDownLatchContext(var count: Long) {
    val latchWaiters = mutableSetOf<Long>()
    fun await(): Boolean {
        if (count > 0) {
            latchWaiters.add(Thread.currentThread().id)
            return true
        }
        assert(count == 0L)
        return false
    }

    fun countDown() {
        // If count is already zero we do not need to
        // do anything
        if (count == 0L) {
            return
        }
        count -= 1
        if (count == 0L) {
            for (tid in latchWaiters) {
                GlobalContext.registeredThreads[tid]!!.pendingOperation = ThreadResumeOperation()
                GlobalContext.registeredThreads[tid]!!.state = ThreadState.Enabled
            }
        }
        return
    }

}