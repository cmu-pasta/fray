package cmu.pasta.sfuzz.core.concurrency

// Simple sync structure to block a thread and wait
// for signals.
class Sync(private var checkSignal: Boolean = false): Object() {
    private var signaled = false
    fun block() {
        if (checkSignal && signaled) {
            signaled = false
            return
        }
        synchronized(this) {
            this.wait()
        }
        signaled = false
    }
    fun unblock() {
        signaled = true
        synchronized(this) {
            this.notify()
        }
    }
}