package cmu.pasta.sfuzz.core.concurrency

// Simple sync structure to block a thread and wait
// for signals.
class Sync(val goal: Int): Any() {
    private var count = 0

    @Synchronized
    fun block() {
        if (count == goal) {
            count = 0
            return
        }
        // We don't need synchronized here because
        // it is already inside a synchronized method
        while (count < goal) {
            try {
                (this as Object).wait()
            } catch (e: InterruptedException) {
                // We should not let Thread.interrupt
                // to unblock this operation.
            }
        }
        // At this point no concurrency.
        count = 0
    }

    @Synchronized
    fun unblock() {
        count += 1
        if (count > goal) {
            println("??")
        }
        assert(count <= goal)
        if (count == goal) {
            (this as Object).notify()
        }
    }
}