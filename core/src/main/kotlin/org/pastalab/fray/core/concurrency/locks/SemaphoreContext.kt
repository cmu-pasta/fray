package org.pastalab.fray.core.concurrency.locks

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.ThreadState
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation

class SemaphoreContext(var totalPermits: Int) : Interruptible {
  private val lockWaiters = mutableMapOf<Long, Pair<Int, LockWaiter>>()

  fun acquire(
      permits: Int,
      shouldBlock: Boolean,
      canInterrupt: Boolean,
      thread: ThreadContext
  ): Boolean {
    if (totalPermits >= permits) {
      totalPermits -= permits
      return true
    } else {
      if (canInterrupt) {
        thread.checkInterrupt()
      }
      if (shouldBlock) {
        lockWaiters[Thread.currentThread().id] = Pair(permits, LockWaiter(canInterrupt, thread))
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
          p.second.thread.pendingOperation = ThreadResumeOperation(true)
          p.second.thread.state = ThreadState.Enabled
          lockWaiters.remove(tid)
        }
      }
    }
  }

  fun reducePermits(permits: Int) {
    totalPermits -= permits
  }

  override fun interrupt(tid: Long) {
    val lockWaiter = lockWaiters[tid] ?: return
    if (lockWaiter.second.canInterrupt) {
      lockWaiter.second.thread.pendingOperation = ThreadResumeOperation(false)
      lockWaiter.second.thread.state = ThreadState.Enabled
      lockWaiters.remove(tid)
    }
  }
}
