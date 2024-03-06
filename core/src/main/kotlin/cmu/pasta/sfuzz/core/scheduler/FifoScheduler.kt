package cmu.pasta.sfuzz.core.scheduler

import cmu.pasta.sfuzz.core.ThreadContext
import cmu.pasta.sfuzz.core.ThreadState

class FifoScheduler: Scheduler {
    override fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext? {
        return threads.sortedBy { it.thread.id }.firstOrNull { it.state == ThreadState.Enabled }
    }
}