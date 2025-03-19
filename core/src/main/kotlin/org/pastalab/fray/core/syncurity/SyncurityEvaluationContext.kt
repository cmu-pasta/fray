package org.anonlab.fray.core.syncurity

import java.util.concurrent.CountDownLatch
import org.anonlab.fray.core.RunContext

class SyncurityEvaluationContext(val runContext: RunContext) {
  fun latchAwait(latch: CountDownLatch) {
    val context = runContext.latchManager.getContext(latch)
    if (context.count > 0) {
      throw AbortEvaluationException("Abort syncurity condition evaluation because of deadlock.")
    }
  }

  fun lockImpl(
      lock: Any,
  ) {
    val lockContext = runContext.lockManager.getContext(lock)
    if (!lockContext.canLock(Thread.currentThread().id)) {
      throw AbortEvaluationException("Abort syncurity condition evaluation because of deadlock.")
    } else {
      return
    }
  }
}
