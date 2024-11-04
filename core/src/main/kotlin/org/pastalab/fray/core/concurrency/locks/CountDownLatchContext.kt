package org.pastalab.fray.core.concurrency.locks

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.ThreadState
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation

class CountDownLatchContext(var count: Long) : Interruptible {
  val latchWaiters = mutableMapOf<Long, LockWaiter>()

  fun await(canInterrupt: Boolean, thread: ThreadContext): Boolean {
    if (count > 0) {
      if (canInterrupt) {
        thread.checkInterrupt()
      }
      latchWaiters[Thread.currentThread().id] = LockWaiter(canInterrupt, thread)
      return true
    }
    assert(count == 0L)
    return false
  }

  override fun interrupt(tid: Long) {
    val lockWaiter = latchWaiters[tid] ?: return
    if (lockWaiter.canInterrupt) {
      lockWaiter.thread.pendingOperation = ThreadResumeOperation(false)
      lockWaiter.thread.state = ThreadState.Enabled
      latchWaiters.remove(tid)
    }
  }

  fun release(): Int {
    if (count == 0L) {
      return 0
    }
    count = 0
    var threads = 0
    for (lockWaiter in latchWaiters.values) {
      lockWaiter.thread.pendingOperation = ThreadResumeOperation(true)
      lockWaiter.thread.state = ThreadState.Enabled
      threads += 1
    }
    return threads
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
      for (lockWaiter in latchWaiters.values) {
        lockWaiter.thread.pendingOperation = ThreadResumeOperation(true)
        lockWaiter.thread.state = ThreadState.Enabled
        threads += 1
      }
      return threads
    }
    return 0
  }
}
