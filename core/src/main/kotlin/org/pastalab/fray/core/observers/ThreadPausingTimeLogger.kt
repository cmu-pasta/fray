package org.pastalab.fray.core.observers

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.logger.FrayLogger
import org.pastalab.fray.rmi.ScheduleObserver

class ThreadPausingTimeLogger(val logger: FrayLogger) : ScheduleObserver<ThreadContext> {
  val threadLastScheduledTime = mutableMapOf<Long, Long>()

  override fun onNewSchedule(allThreads: Collection<ThreadContext>, scheduled: ThreadContext) {}

  override fun onContextSwitch(current: ThreadContext, next: ThreadContext) {
    val currentThreadId = current.thread.id
    val nextThreadId = next.thread.id
    val nanoTime = System.nanoTime()
    threadLastScheduledTime[currentThreadId] = nanoTime
    val message =
        "Context switch: $currentThreadId is now paused, $nextThreadId is running. " +
            if (nextThreadId !in threadLastScheduledTime) {
              "Last scheduled time not recorded for $nextThreadId."
            } else {
              val lastScheduledTime = threadLastScheduledTime[nextThreadId]!!
              val pauseDuration =
                  (System.nanoTime() - lastScheduledTime) / 1_000_000 // Convert to milliseconds
              "Pause duration: $pauseDuration ms"
            }
    logger.info(message)
  }
}
