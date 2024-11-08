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

  fun unblockThread(tid: Long, isTimeout: Boolean, isInterrupt: Boolean) {
    val lockWaiter = latchWaiters[tid] ?: return
    if (isInterrupt && !lockWaiter.canInterrupt) {
      return
    }
    lockWaiter.thread.pendingOperation = ThreadResumeOperation(!isTimeout)
    lockWaiter.thread.state = ThreadState.Enabled
    latchWaiters.remove(tid)
  }

  override fun interrupt(tid: Long) {
    unblockThread(tid, false, true)
  }

  fun release(): Int {
    if (count == 0L) {
      return 0
    }
    count = 0
    var threads = 0
    for (lockWaiter in latchWaiters.values.toList()) {
      unblockThread(lockWaiter.thread.thread.id, false, false)
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
      for (lockWaiter in latchWaiters.values.toList()) {
        unblockThread(lockWaiter.thread.thread.id, false, false)
        threads += 1
      }
      return threads
    }
    return 0
  }
}
