package org.pastalab.fray.core.concurrency.locks

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.ThreadState
import org.pastalab.fray.core.concurrency.operations.CountDownLatchAwaitBlocking
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation

class CountDownLatchContext(var count: Long) {
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

  fun unblockThread(tid: Long, isTimeout: Boolean, isInterrupt: Boolean): Boolean {
    val lockWaiter = latchWaiters[tid] ?: return false
    if (isInterrupt && !lockWaiter.canInterrupt) {
      return false
    }
    val pendingOperation = lockWaiter.thread.pendingOperation
    assert(pendingOperation is CountDownLatchAwaitBlocking)
    lockWaiter.thread.pendingOperation = ThreadResumeOperation(!isTimeout)
    lockWaiter.thread.state = ThreadState.Enabled
    latchWaiters.remove(tid)
    if ((pendingOperation as CountDownLatchAwaitBlocking).timed) {
      return false
    } else {
      return true
    }
  }

  fun release(): Int {
    if (count == 0L) {
      return 0
    }
    count = 0
    var threads = 0
    for (lockWaiter in latchWaiters.values.toList()) {
      if (unblockThread(lockWaiter.thread.thread.id, false, false)) {
        threads += 1
      }
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
        if (unblockThread(lockWaiter.thread.thread.id, false, false)) {
          threads += 1
        }
      }
      return threads
    }
    return 0
  }
}
