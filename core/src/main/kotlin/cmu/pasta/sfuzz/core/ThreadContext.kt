package cmu.pasta.sfuzz.cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.concurrency.Sync
import java.util.concurrent.Semaphore

enum class ThreadState {
    Enabled,
    Running,
    Paused,
    Completed
}
class ThreadContext(val thread: Thread) {
    var state = ThreadState.Enabled
    val sync = Sync(1)
    fun block() {
        sync.block()
    }
    fun unblock() {
        sync.unblock()
    }
}