package cmu.pasta.fray.core.scheduler

import cmu.pasta.fray.core.ThreadContext
import cmu.pasta.fray.core.math.Utils

class PCTScheduler(val rand: ControlledRandom, val numSwitchPoints: Int) : Scheduler {

  var currentStep = 0
  var nextSwitchPoint = 0
  var maxStep = 0
  var numSwitchPointLeft = numSwitchPoints
  val threadPriorityQueue = mutableListOf<ThreadContext>()

  init {
    updateNextSwitchPoint()
  }

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

  override fun scheduleNextOperation(threads: List<ThreadContext>): ThreadContext {
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

  override fun done() {
    maxStep = maxStep.coerceAtLeast(currentStep)
    currentStep = 0
    numSwitchPointLeft = numSwitchPoints
    nextSwitchPoint = 0
    threadPriorityQueue.clear()
    rand.done()
    updateNextSwitchPoint()
  }
}
