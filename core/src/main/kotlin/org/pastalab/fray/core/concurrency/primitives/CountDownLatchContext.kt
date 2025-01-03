package org.pastalab.fray.core.concurrency.primitives

import java.util.concurrent.CountDownLatch
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.SynchronizationManager
import org.pastalab.fray.core.concurrency.operations.CountDownLatchAwaitBlocking
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation
import org.pastalab.fray.core.utils.Utils.verifyOrReport
import org.pastalab.fray.rmi.ThreadState

/**
 * Context for a [CountDownLatch]. We have to pass syncManager here because we need to create a
 * synchronization point when we unblock a thread forcefully.
 */
class CountDownLatchContext(val latch: CountDownLatch, val syncManager: SynchronizationManager) :
    InterruptibleContext {
  var count = latch.count
  val latchWaiters = mutableMapOf<Long, LockWaiter>()

  fun await(canInterrupt: Boolean, thread: ThreadContext): Boolean {
    if (count > 0) {
      if (canInterrupt) {
        thread.checkInterrupt()
      }
      latchWaiters[Thread.currentThread().id] = LockWaiter(canInterrupt, thread)
      return true
    }
    verifyOrReport(count == 0L)
    return false
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
        if (unblockThread(lockWaiter.thread.thread.id, InterruptionType.RESOURCE_AVAILABLE)) {
          threads += 1
        }
      }
      return threads
    }
    return 0
  }

  override fun unblockThread(tid: Long, type: InterruptionType): Boolean {
    val lockWaiter = latchWaiters[tid] ?: return false
    if (type == InterruptionType.INTERRUPT && !lockWaiter.canInterrupt) {
      return false
    }
    if (type == InterruptionType.FORCE) {
      while (count != 0L) {
        val unblockedThreads = countDown()
        if (unblockedThreads > 0) {
          syncManager.createWait(latch, unblockedThreads)
          latch.countDown()
          syncManager.wait(latch)
        } else {
          latch.countDown()
        }
      }
      return false
    }
    val pendingOperation = lockWaiter.thread.pendingOperation
    verifyOrReport(pendingOperation is CountDownLatchAwaitBlocking)
    lockWaiter.thread.pendingOperation = ThreadResumeOperation(type != InterruptionType.TIMEOUT)
    lockWaiter.thread.state = ThreadState.Enabled
    latchWaiters.remove(tid)
    return if ((pendingOperation as CountDownLatchAwaitBlocking).timed) {
      false
    } else {
      true
    }
  }
}
