package cmu.pasta.sfuzz.core.scheduler

import cmu.pasta.sfuzz.core.ThreadContext

class RandomScheduler : Scheduler {
  var rand = ControlledRandom(arrayListOf(1, 2, 3, 2, 3, 3, 3, 3))

  override fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext {

    if (threads.size == 1) {
      return threads[0]
    }
    val index = rand.nextInt() % threads.size
    return threads[index]
  }

  override fun done() {
    rand = ControlledRandom()
  }
}
