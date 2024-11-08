package org.pastalab.fray.core.concurrency.locks

import java.util.concurrent.CountDownLatch
import org.pastalab.fray.core.ThreadContext

class CountDownLatchManager {
  val latchStore = ReferencedContextManager { it ->
    if (it is CountDownLatch) {
      CountDownLatchContext(it.count)
    } else {
      throw IllegalArgumentException("CountDownLatchManager can only manage CountDownLatch objects")
    }
  }

  fun await(latch: CountDownLatch, canInterrupt: Boolean, thread: ThreadContext): Boolean {
    return latchStore.getContext(latch).await(canInterrupt, thread)
  }

  fun unblockThread(latch: CountDownLatch, tid: Long, isTimeout: Boolean, isInterrupt: Boolean) {
    latchStore.getContext(latch).unblockThread(tid, isTimeout, isInterrupt)
  }

  /*
   * Returns number of unblocked threads.
   */
  fun countDown(latch: CountDownLatch): Int {
    return latchStore.getContext(latch).countDown()
  }

  fun release(latch: CountDownLatch): Int {
    return latchStore.getContext(latch).release()
  }
}
