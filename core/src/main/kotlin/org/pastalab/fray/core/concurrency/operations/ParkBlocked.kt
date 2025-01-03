package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.rmi.ThreadState
import org.pastalab.fray.core.concurrency.primitives.InterruptionType
import org.pastalab.fray.core.utils.Utils.verifyOrReport

class ParkBlocked(timed: Boolean, val threadContext: ThreadContext) :
    TimedBlockingOperation(timed) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    verifyOrReport(tid == threadContext.thread.id) { "Thread id mismatch" }
    threadContext.pendingOperation = ThreadResumeOperation(type != InterruptionType.TIMEOUT)
    threadContext.state = ThreadState.Enabled
    return null
  }
}
