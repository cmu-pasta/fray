package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.concurrency.Sync
import cmu.pasta.sfuzz.core.concurrency.operations.Operation
import cmu.pasta.sfuzz.core.concurrency.operations.ThreadStartOperation

enum class ThreadState {
    Enabled,
    Running,
    Paused,
    Parked,
    Completed,

    // Thread is started but not yet available.
    STARTED,
}

class ThreadContext(val thread: Thread, val index: Int) {
    var state = ThreadState.STARTED
    var unparkSignaled = false

    // Pending operation is null if a thread is just resumed/blocked.
    var pendingOperation: Operation = ThreadStartOperation()
    val sync = Sync(1)
    fun block() {
        sync.block()
    }
    fun unblock() {
        sync.unblock()
    }
}