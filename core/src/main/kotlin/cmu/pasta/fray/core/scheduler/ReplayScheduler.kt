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
    if (threads.map { it.index }.toList() != choice.enabledIds) {
      println("?")
    }
    assert(threads.map { it.index }.toList() == choice.enabledIds)
    assert(choice.enabled == threads.size)

    val selected = threads[choice.selected % threads.size]
    //    assert(choice.threadId == selected.index)
    index += 1
    if (selected.pendingOperation.toString().split("@")[0] != choice.operation.split("@")[0]) {
      println("?")
    }
    assert(selected.pendingOperation.toString() == choice.operation)
    return selected
  }

  override fun done() {}
}
