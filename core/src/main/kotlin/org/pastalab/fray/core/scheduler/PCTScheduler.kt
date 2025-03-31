package org.pastalab.fray.core.scheduler

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.randomness.ControlledRandom

@Serializable
class PCTScheduler(val rand: ControlledRandom, val numSwitchPoints: Int, var maxStep: Int) :
    Scheduler {
  constructor() : this(ControlledRandom(), 3, 0) {}

  @Transient var currentStep = 0
  @Transient val threadPriorityQueue = mutableListOf<ThreadContext>()
  @Transient val priorityChangePoints = mutableSetOf<Int>()

  init {
    if (maxStep != 0) {
      preparePriorityChangePoints()
    }
  }

  override fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: List<ThreadContext>
  ): ThreadContext {
    if (threads.size == 1) {
      return threads[0]
    }
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
    if (priorityChangePoints.contains(currentStep)) {
      threadPriorityQueue.remove(next)
      threadPriorityQueue.add(next)
    }
    return next
  }

  override fun nextIteration(): Scheduler {
    return PCTScheduler(ControlledRandom(), numSwitchPoints, maxStep.coerceAtLeast(currentStep))
  }

  private fun preparePriorityChangePoints() {
    val listOfInts = (1..maxStep).toMutableList()
    for (i in 0 ..< numSwitchPoints) {
      val index = rand.nextInt() % (listOfInts.size)
      priorityChangePoints.add(listOfInts[index])
      listOfInts.removeAt(index)
      if (listOfInts.isEmpty()) break
    }
  }
}
