package cmu.pasta.fray.core.scheduler

import cmu.pasta.fray.core.ThreadContext

class RandomScheduler : Scheduler {
  var rand = ControlledRandom()

  override fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext {

    if (threads.size == 1) {
      return threads[0]
    }
    val index = rand.nextInt() % threads.size
    return threads[index]
  }

  override fun done() {
    rand.done()
  }
}