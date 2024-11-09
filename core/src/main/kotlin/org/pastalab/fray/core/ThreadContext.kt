package org.pastalab.fray.core

import org.pastalab.fray.core.concurrency.Sync
import org.pastalab.fray.core.concurrency.operations.ConditionAwaitBlocked
import org.pastalab.fray.core.concurrency.operations.ObjectWaitBlock
import org.pastalab.fray.core.concurrency.operations.Operation
import org.pastalab.fray.core.concurrency.operations.ThreadStartOperation

enum class ThreadState {
  Enabled,
  Running,
  Paused,
  Completed,
}

class ThreadContext(val thread: Thread, val index: Int, context: RunContext) {
  val localRandomProbe = context.config.randomnessProvider.nextInt()
  var state = ThreadState.Paused
  var unparkSignaled = false
  var interruptSignaled = false
  var isExiting = false

  // Pending operation is null if a thread is just resumed/blocked.
  var pendingOperation: Operation = ThreadStartOperation()
  val sync = Sync(1)

  fun block() {
    sync.block()
  }

  fun schedulable() = state == ThreadState.Enabled || state == ThreadState.Running

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

  var lastBlockedOp = -1
  var blockedTime = 0

  fun maybeLiveLock(): Boolean {
    val op = pendingOperation
    when (op) {
      is ObjectWaitBlock -> {
        if (System.identityHashCode(op.o) == lastBlockedOp) {
          blockedTime += 1
        } else {
          blockedTime = 1
          lastBlockedOp = System.identityHashCode(op.o)
        }
      }
      is ConditionAwaitBlocked -> {
        if (System.identityHashCode(op.condition) == lastBlockedOp) {
          blockedTime += 1
        } else {
          blockedTime = 1
          lastBlockedOp = System.identityHashCode(op.condition)
        }
      }
    }
    return blockedTime > 1000
  }
}
