package org.pastalab.fray.core.scheduler

import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.randomness.Randomness

class PCTScheduler(rand: Randomness, val numSwitchPoints: Int, var maxStep: Int) : Scheduler(rand) {
  constructor() : this(ControlledRandom(), 3, 0)

  // Single-argument constructor used to reconstruct the scheduler for replay.
  constructor(rand: Randomness) : this(rand, 3, 0)

  var currentStep = 0
  val threadPriorityQueue = mutableListOf<ThreadContext>()
  val priorityChangePoints = mutableSetOf<Int>()

  init {
    if (maxStep != 0) {
      preparePriorityChangePoints()
    }
  }

  override fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: Collection<ThreadContext>,
  ): ThreadContext {
    if (threads.size == 1) {
      return threads[0]
    }
    currentStep += 1
    for (thread in threads) {
      if (!threadPriorityQueue.contains(thread)) {
        if (threadPriorityQueue.isEmpty()) {
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

  override fun nextIteration(randomness: Randomness): Scheduler {
    return PCTScheduler(randomness, numSwitchPoints, maxStep.coerceAtLeast(currentStep))
  }

  private fun preparePriorityChangePoints() {
    val listOfInts = (1..maxStep).toMutableList()
    for (i in 0..<numSwitchPoints) {
      val index = rand.nextInt() % (listOfInts.size)
      priorityChangePoints.add(listOfInts[index])
      listOfInts.removeAt(index)
      if (listOfInts.isEmpty()) break
    }
  }
}
