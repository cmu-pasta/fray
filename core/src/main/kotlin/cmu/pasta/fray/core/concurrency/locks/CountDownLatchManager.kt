package cmu.pasta.fray.core.concurrency.locks

import java.util.concurrent.CountDownLatch

class CountDownLatchManager {
  val latchStore = ReferencedContextManager { it ->
    if (it is CountDownLatch) {
      CountDownLatchContext(it.count)
    } else {
      throw IllegalArgumentException("CountDownLatchManager can only manage CountDownLatch objects")
    }
  }

  fun await(latch: CountDownLatch, canInterrupt: Boolean): Boolean {
    return latchStore.getLockContext(latch).await(canInterrupt)
  }

  /*
   * Returns number of unblocked threads.
   */
  fun countDown(latch: CountDownLatch): Int {
    return latchStore.getLockContext(latch).countDown()
  }
}
