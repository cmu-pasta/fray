package cmu.pasta.sfuzz.core.scheduler

import cmu.pasta.sfuzz.core.ThreadContext
import cmu.pasta.sfuzz.core.exception.SchedulerInternalException
import kotlinx.serialization.json.Json

class ReplayScheduler(val schedule: Schedule) : Scheduler {

  var index = 0

  override fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext {
    if (threads.size == 1 && !schedule.fullSchedule) {
      return threads[0]
    }
    if (index >= schedule.choices.size) {
      throw SchedulerInternalException("Require more scheduling choices for replay scheduler")
    }
    val choice = schedule.choices[index]
    assert(choice.enabled == threads.size)
    val selected = threads[choice.selected]
    assert(choice.threadId == selected.index)
    index += 1
    return selected
  }

  override fun done() {
  }
}
