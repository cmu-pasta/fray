package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.concurrency.Sync
import cmu.pasta.sfuzz.core.concurrency.operations.Operation
import cmu.pasta.sfuzz.core.concurrency.operations.ThreadStartOperation

enum class ThreadState {
    Enabled,
    Running,
    Paused,
    Completed
}
class ThreadContext(val thread: Thread) {
    var state = ThreadState.Enabled

    // Pending operation is null if a thread is just resumed/blocked.
    var pendingOperation: Operation? = ThreadStartOperation()
    val sync = Sync(1)
    fun block() {
        sync.block()
    }
    fun unblock() {
        sync.unblock()
    }
}