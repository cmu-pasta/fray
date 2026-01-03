package org.pastalab.fray.core.scheduler

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.observers.ScheduleRecording
import org.pastalab.fray.core.randomness.Randomness
import org.pastalab.fray.core.utils.Utils.verifyOrReport

class ReplayScheduler(val recording: List<ScheduleRecording>) : Scheduler {
  @Transient var index = 0

  override fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: Collection<ThreadContext>,
  ): ThreadContext {
    if (index >= recording.size) {
      return threads[0]
    }
    val rec = recording[index]
    val thread = threads.firstOrNull { it.index == rec.scheduled }
    verifyOrReport(
        thread != null,
        "The scheduled thread ${rec.scheduled} is not in the current thread list.",
    )
    index++
    return thread
  }

  override fun nextIteration(randomness: Randomness): Scheduler {
    return ReplayScheduler(recording)
  }
}
