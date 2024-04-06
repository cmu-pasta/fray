package cmu.pasta.sfuzz.core.concurrency.locks

import cmu.pasta.sfuzz.core.GlobalContext
import cmu.pasta.sfuzz.core.ThreadState
import cmu.pasta.sfuzz.core.concurrency.operations.ThreadResumeOperation

class CountDownLatchContext(var count: Long) : Interruptible {
  val latchWaiters = mutableMapOf<Long, Boolean>()

  fun await(canInterrupt: Boolean): Boolean {
    if (count > 0) {
      if (canInterrupt) {
        GlobalContext.registeredThreads[Thread.currentThread().id]?.checkInterrupt()
      }
      latchWaiters[Thread.currentThread().id] = canInterrupt
      return true
    }
    assert(count == 0L)
    return false
  }

  override fun interrupt(tid: Long) {
    if (latchWaiters[tid] == true) {
      GlobalContext.registeredThreads[tid]!!.pendingOperation = ThreadResumeOperation()
      GlobalContext.registeredThreads[tid]!!.state = ThreadState.Enabled
      latchWaiters.remove(tid)
    }
  }

  /*
   * Returns number of unblocked threads.
   */
  fun countDown(): Int {
    // If count is already zero we do not need to
    // do anything
    if (count == 0L) {
      return 0
    }
    count -= 1
    if (count == 0L) {
      var threads = 0
      for (tid in latchWaiters.keys) {
        GlobalContext.registeredThreads[tid]!!.pendingOperation = ThreadResumeOperation()
        GlobalContext.registeredThreads[tid]!!.state = ThreadState.Enabled
        threads += 1
      }
      return threads
    }
    return 0
  }
}
