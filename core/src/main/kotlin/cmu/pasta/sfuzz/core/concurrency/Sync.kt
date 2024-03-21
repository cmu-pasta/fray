package cmu.pasta.sfuzz.core.concurrency

// Simple sync structure to block a thread and wait
// for signals.
class Sync(val goal: Int): Any() {
    private var count = 0

    @Synchronized
    fun block(): Boolean {
        if (count == goal) {
            count = 0
            return false
        }
        var interruptSignaled = false
        // We don't need synchronized here because
        // it is already inside a synchronized method
        while (count < goal) {
            try {
                (this as Object).wait()
            } catch (e: InterruptedException) {
                interruptSignaled = true
                println("Thread interrupted!")
            }
        }
        // At this point no concurrency.
        count = 0
        return interruptSignaled
    }

    @Synchronized
    fun unblock() {
        count += 1
        assert(count <= goal)
        if (count == goal) {
            (this as Object).notify()
        }
    }
}