package cmu.pasta.sfuzz.core.concurrency

// Simple sync structure to block a thread and wait
// for signals.
class Sync(val goal: Int): Object() {
    private var count = 0
    @Synchronized
    fun block() {
        if (count == goal) {
            count = 0
            return
        }
        // We don't need synchronized here because
        // it is already inside a synchronized method
        this.wait()
        // At this point no concurrency.
        count = 0
    }

    @Synchronized
    fun unblock() {
        count += 1
        if (count == goal) {
            this.notify()
        }
    }
}