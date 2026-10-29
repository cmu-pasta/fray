package org.pastalab.fray.core.scheduler

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.observers.ScheduleRecording
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.randomness.Randomness

class ReplayScheduler(
    val recording: List<ScheduleRecording>,
    rand: Randomness = ControlledRandom(),
) : Scheduler(rand) {
  var index = 0

  override fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: Collection<ThreadContext>,
  ): ThreadContext {
    if (index >= recording.size) {
      return threads[0]
    }
    val rec = recording[index]
    val thread =
        threads.firstOrNull { it.index == rec.scheduled }
            ?: throw IllegalStateException(
                "Replay diverged at recording index $index: scheduled thread ${rec.scheduled} " +
                    "is not runnable; runnable threads: ${threads.map { it.index }}"
            )
    index++
    return thread
  }

  override fun nextIteration(randomness: Randomness): Scheduler {
    return ReplayScheduler(recording)
  }
}
