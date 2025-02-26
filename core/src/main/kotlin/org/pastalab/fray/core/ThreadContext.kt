package org.pastalab.fray.core

import org.pastalab.fray.core.concurrency.Sync
import org.pastalab.fray.core.concurrency.operations.Operation
import org.pastalab.fray.core.concurrency.operations.ThreadStartOperation
import org.pastalab.fray.core.utils.isFrayInternals
import org.pastalab.fray.rmi.ThreadInfo
import org.pastalab.fray.rmi.ThreadState

class ThreadContext(val thread: Thread, val index: Int, context: RunContext) {
  val localRandomProbe = context.config.randomnessProvider.nextInt()
  var state = ThreadState.Created
  var unparkSignaled = false
  var interruptSignaled = false
  var isExiting = false

  // Pending operation is null if a thread is just resumed/blocked.
  var pendingOperation: Operation = ThreadStartOperation()
  val sync = Sync(1)

  fun block() {
    sync.block()
  }

  fun schedulable() = state == ThreadState.Runnable || state == ThreadState.Running

  fun unblock() {
    sync.unblock()
  }

  fun checkInterrupt() {
    if (interruptSignaled) {
      interruptSignaled = false
      Thread.interrupted()
      throw InterruptedException()
    }
  }

  fun toStackInfo(): ThreadInfo {
    val stackTraces =
        when (pendingOperation) {
          is ThreadStartOperation -> {
            listOf(
                StackTraceElement(
                    "ThreadStartOperation", "ThreadStartOperation", "ThreadStartOperation", 0))
          }
          else -> thread.stackTrace.toList().drop(1).filter { !it.isFrayInternals }
        }
    return ThreadInfo(thread.name, index.toLong(), state, stackTraces)
  }
}
