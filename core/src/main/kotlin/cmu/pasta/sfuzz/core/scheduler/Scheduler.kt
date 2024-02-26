package cmu.pasta.sfuzz.core.scheduler

import cmu.pasta.sfuzz.cmu.pasta.sfuzz.core.ThreadContext

interface Scheduler {
    fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext?
}