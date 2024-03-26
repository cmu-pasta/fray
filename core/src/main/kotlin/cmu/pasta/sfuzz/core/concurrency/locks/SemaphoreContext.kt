package cmu.pasta.sfuzz.core.concurrency.locks

import cmu.pasta.sfuzz.core.GlobalContext
import cmu.pasta.sfuzz.core.ThreadState
import cmu.pasta.sfuzz.core.concurrency.operations.ThreadResumeOperation

class SemaphoreContext(var totalPermits: Int) {
  private val lockWaiters = mutableMapOf<Long, Int>()

  fun acquire(permits: Int, shouldBlock: Boolean): Boolean {
    if (totalPermits > permits) {
      totalPermits -= permits
      return true
    } else if (shouldBlock) {
      lockWaiters[Thread.currentThread().id] = permits
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
        if (totalPermits >= p) {
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
}
