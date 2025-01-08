package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.primitives.InterruptionType
import org.pastalab.fray.rmi.ThreadState

class ThreadSleepBlocking(val context: ThreadContext) : TimedBlockingOperation(true) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    context.pendingOperation = ThreadResumeOperation(type != InterruptionType.TIMEOUT)
    context.state = ThreadState.Enabled
    return null
  }
}
