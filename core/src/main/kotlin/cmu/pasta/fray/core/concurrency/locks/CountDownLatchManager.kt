package cmu.pasta.fray.core.concurrency.locks

import cmu.pasta.fray.core.ThreadContext
import java.util.concurrent.CountDownLatch

class CountDownLatchManager {
  val latchStore = ReferencedContextManager { it ->
    if (it is CountDownLatch) {
      CountDownLatchContext(it.count)
    } else {
      throw IllegalArgumentException("CountDownLatchManager can only manage CountDownLatch objects")
    }
  }

  fun await(latch: CountDownLatch, canInterrupt: Boolean, thread: ThreadContext): Boolean {
    return latchStore.getLockContext(latch).await(canInterrupt, thread)
  }

  /*
   * Returns number of unblocked threads.
   */
  fun countDown(latch: CountDownLatch): Int {
    return latchStore.getLockContext(latch).countDown()
  }

  fun release(latch: CountDownLatch): Int {
    return latchStore.getLockContext(latch).release()
  }
}
