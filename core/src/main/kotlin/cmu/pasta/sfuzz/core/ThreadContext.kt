package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.concurrency.Sync
import cmu.pasta.sfuzz.core.concurrency.operations.Operation
import cmu.pasta.sfuzz.core.concurrency.operations.ThreadStartOperation

enum class ThreadState {
  Enabled,
  Running,
  Paused,
  Completed,
}

class ThreadContext(val thread: Thread, val index: Int) {
  var state = ThreadState.Paused
  var unparkSignaled = false
  var interruptSignaled = false

  // This field is set when a thread is resumed by `o.notify()`
  // but hasn't acquire the monitor lock.
  var blockedBy: Any? = null

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
      throw InterruptedException()
    }
  }
}
