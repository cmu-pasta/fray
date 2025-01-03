package org.pastalab.fray.core.scheduler

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.utils.Utils

@Serializable
class PCTScheduler(val rand: ControlledRandom, val numSwitchPoints: Int, var maxStep: Int) :
    Scheduler {
  constructor() : this(ControlledRandom(), 3, 0) {}

  @Transient var currentStep = 0
  @Transient var nextSwitchPoint = 0
  @Transient var numSwitchPointLeft = numSwitchPoints
  @Transient val threadPriorityQueue = mutableListOf<ThreadContext>()

  fun updateNextSwitchPoint() {
    numSwitchPointLeft -= 1
    if (numSwitchPoints == 0) return
    val switchPointProbability =
        if (maxStep == 0) {
          0.1
        } else {
          1.0 * numSwitchPointLeft / maxStep
        }
    nextSwitchPoint += Utils.sampleGeometric(switchPointProbability, rand.nextDouble())
  }

  override fun scheduleNextOperation(threads: List<ThreadContext>, allThreads: List<ThreadContext>): ThreadContext {
    currentStep += 1
    for (thread in threads) {
      if (!threadPriorityQueue.contains(thread)) {
        if (threadPriorityQueue.size == 0) {
          threadPriorityQueue.add(thread)
          continue
        }
        val index = rand.nextInt() % (threadPriorityQueue.size + 1)
        threadPriorityQueue.add(index, thread)
      }
    }
    val next = threadPriorityQueue.first { threads.contains(it) }
    if (currentStep == nextSwitchPoint) {
      threadPriorityQueue.remove(next)
      threadPriorityQueue.add(next)
      updateNextSwitchPoint()
    }
    return next
  }

  override fun nextIteration(): Scheduler {
    return PCTScheduler(ControlledRandom(), numSwitchPoints, maxStep.coerceAtLeast(currentStep))
  }
}
