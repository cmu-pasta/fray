package cmu.pasta.fray.core.concurrency

// Simple sync structure to block a thread and wait
// for signals.
class Sync(val goal: Int) : Any() {
  private var count = 0
  private var isBlocked = false

  @Synchronized fun isBlocked() = isBlocked

  @Synchronized
  fun block() {
    if (count == goal) {
      count = 0
      return
    }
    isBlocked = true
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
    isBlocked = false
    // At this point no concurrency.
    count = 0
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
