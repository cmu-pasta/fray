package org.pastalab.fray.core.syncurity

import org.pastalab.fray.core.RunContext

class SyncurityEvaluationContext(val runContext: RunContext) {
  fun lockImpl(
      lock: Any,
  ) {
    val lockContext = runContext.lockManager.getContext(lock)
    if (!lockContext.canLock(Thread.currentThread().id)) {
      throw RuntimeException("Abort syncurity condition evaluation because of deadlock.")
    } else {
      return
    }
  }
}
