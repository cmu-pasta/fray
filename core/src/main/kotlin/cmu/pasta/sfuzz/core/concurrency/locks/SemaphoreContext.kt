package cmu.pasta.sfuzz.core.concurrency.locks

import cmu.pasta.sfuzz.core.GlobalContext
import cmu.pasta.sfuzz.core.ThreadState
import cmu.pasta.sfuzz.core.concurrency.operations.ThreadResumeOperation

class SemaphoreContext(var totalPermits: Int) : Interruptible {
  private val lockWaiters = mutableMapOf<Long, Pair<Int, Boolean>>()

  fun acquire(permits: Int, shouldBlock: Boolean, canInterrupt: Boolean): Boolean {
    if (totalPermits >= permits) {
      totalPermits -= permits
      return true
    } else {
      if (canInterrupt) {
        GlobalContext.registeredThreads[Thread.currentThread().id]?.checkInterrupt()
      }
      if (shouldBlock) {
        lockWaiters[Thread.currentThread().id] = Pair(permits, canInterrupt)
      }
    }
    return false
  }

  fun drainPermits(): Int {
    val permits = totalPermits
    totalPermits = 0
    return permits
  }

  fun release(permits: Int) {
    totalPermits += permits
    if (totalPermits > 0) {
      val it = lockWaiters.iterator()
      while (it.hasNext()) {
        val (tid, p) = it.next()
        if (totalPermits >= p.first) {
          GlobalContext.registeredThreads[tid]!!.pendingOperation = ThreadResumeOperation()
          GlobalContext.registeredThreads[tid]!!.state = ThreadState.Enabled
          lockWaiters.remove(tid)
        }
      }
    }
  }

  fun reducePermits(permits: Int) {
    totalPermits -= permits
  }

  override fun interrupt(tid: Long) {
    if (lockWaiters[tid]?.second == true) {
      GlobalContext.registeredThreads[tid]!!.pendingOperation = ThreadResumeOperation()
      GlobalContext.registeredThreads[tid]!!.state = ThreadState.Enabled
      lockWaiters.remove(tid)
    }
  }
}
