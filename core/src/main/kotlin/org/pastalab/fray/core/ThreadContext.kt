package org.pastalab.fray.core

import org.pastalab.fray.core.concurrency.context.Acquirable
import org.pastalab.fray.core.concurrency.operations.BlockedOperation
import org.pastalab.fray.core.concurrency.operations.Operation
import org.pastalab.fray.core.concurrency.operations.ThreadStartOperation
import org.pastalab.fray.core.utils.Sync
import org.pastalab.fray.core.utils.isFrayInternals
import org.pastalab.fray.rmi.ThreadInfo
import org.pastalab.fray.rmi.ThreadState

class ThreadContext(val thread: Thread, val index: Int, context: RunContext, parentId: Int) {
  val localRandomProbe = context.config.randomness.nextInt()
  var state = ThreadState.Created
  var unparkSignaled = false
  var interruptSignaled = false
  var isExiting = false
  val acquiredResources = mutableSetOf<Acquirable>()

  var pendingOperation: Operation = ThreadStartOperation(parentId)
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

  fun toThreadInfo(): ThreadInfo {
    val stackTraces =
        when (pendingOperation) {
          is ThreadStartOperation -> {
            listOf(
                StackTraceElement(
                    "ThreadStartOperation",
                    "ThreadStartOperation",
                    "ThreadStartOperation",
                    0,
                )
            )
          }
          else -> thread.stackTrace.toList().drop(1).filter { !it.isFrayInternals }
        }.toMutableList()
    if (stackTraces.isEmpty()) {
      stackTraces +=
          StackTraceElement("ThreadEndOperation", "ThreadEndOperation", "ThreadEndOperation", 0)
    }
    val blockedBy = (pendingOperation as? BlockedOperation)?.resourceInfo
    return ThreadInfo(
        thread.name,
        index,
        thread.id,
        state,
        stackTraces,
        blockedBy,
        acquiredResources.map { it.resourceInfo }.toSet(),
    )
  }
}
