package cmu.pasta.fray.core.scheduler

import cmu.pasta.fray.core.ThreadContext

class ReplayScheduler(val schedule: Schedule) : Scheduler {

  var index = 0

  override fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext {
    if (threads.size == 1 && !schedule.fullSchedule) {
      return threads[0]
    }
    if (index >= schedule.choices.size) {
      return threads[0]
      //      throw SchedulerInternalException("Require more scheduling choices for replay
      // scheduler")
    }
    val choice = schedule.choices[index]
    assert(choice.enabled == threads.size)
    val selected = threads[choice.selected]
    assert(choice.threadId == selected.index)
    index += 1
    return selected
  }

  override fun done() {}
}