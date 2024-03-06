package cmu.pasta.sfuzz.core.scheduler

import cmu.pasta.sfuzz.core.ThreadContext
import cmu.pasta.sfuzz.core.exception.SchedulerInternalException

class ReplayScheduler(val choices: List<Choice>) : Scheduler {
    var index = 0;
    override fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext? {
        if (threads.size == 1) {
            return threads[0]
        }
        if (index > choices.size) {
            print("Require more scheduling choices for replay scheduler !!!!!")
            throw SchedulerInternalException("Require more scheduling choices for replay scheduler")
        }
        val choice = choices[index];
//        assert(choice.enabled == threads.size)
        val selected = threads[choice.selected]
//        assert(choice.threadId == selected.thread.id)
        index += 1
        return selected

    }
}