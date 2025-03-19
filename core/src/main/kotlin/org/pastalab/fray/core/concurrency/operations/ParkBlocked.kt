package org.anonlab.fray.core.concurrency.operations

import org.anonlab.fray.core.ThreadContext
import org.anonlab.fray.core.utils.Utils.verifyOrReport
import org.anonlab.fray.rmi.ResourceInfo
import org.anonlab.fray.rmi.ResourceType
import org.anonlab.fray.rmi.ThreadState

class ParkBlocked(timed: Boolean, val threadContext: ThreadContext) :
    BlockedOperation(timed, ResourceInfo(threadContext.thread.id.toInt(), ResourceType.PARK)) {
  override fun unblockThread(tid: Long, type: InterruptionType): Any? {
    verifyOrReport(tid == threadContext.thread.id) { "Thread id mismatch" }
    threadContext.pendingOperation = ThreadResumeOperation(type != InterruptionType.TIMEOUT)
    threadContext.state = ThreadState.Runnable
    return null
  }
}
