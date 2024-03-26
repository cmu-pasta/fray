package cmu.pasta.sfuzz.core.concurrency.locks

import java.util.concurrent.CountDownLatch

class CountDownLatchManager {
  val latchStore = ReferencedContextManager { it ->
    if (it is CountDownLatch) {
      CountDownLatchContext(it.count)
    } else {
      throw IllegalArgumentException("CountDownLatchManager can only manage CountDownLatch objects")
    }
  }

  fun await(latch: CountDownLatch): Boolean {
    return latchStore.getLockContext(latch).await()
  }

  fun countDown(latch: CountDownLatch) {
    latchStore.getLockContext(latch).countDown()
  }
}
