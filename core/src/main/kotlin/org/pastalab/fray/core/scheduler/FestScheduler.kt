package org.pastalab.fray.core.scheduler

import java.util.Random
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.observers.TimelineCoverage
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.randomness.Randomness

/** Feedback-guided Adaptive Testing of Distributed Systems Designs (Fest, NSDI '26). */
class FestScheduler(
    val base: Scheduler,
    val mutationCount: Int = DEFAULT_MUTATION_COUNT,
    val mutationBudget: Int = DEFAULT_MUTATION_BUDGET,
) : Scheduler(base.rand) {
  constructor() : this(POSScheduler())

  var exploration: FestExploration = FestExploration()
  var coverage: TimelineCoverage? = null

  override fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: Collection<ThreadContext>,
  ): ThreadContext {
    return base.scheduleNextOperation(threads, allThreads)
  }

  override fun nextIteration(randomness: Randomness): Scheduler {
    val state = exploration
    val executed = FestRecording.capture(rand as? ControlledRandom ?: ControlledRandom())

    val currentCoverage = coverage?.getCoverage() ?: (state.lastCoverage + 1)
    if (state.corpus.isEmpty() || currentCoverage > state.lastCoverage) {
      state.corpus.add(executed)
    }
    state.lastCoverage = currentCoverage

    // Each corpus entry is mutated [mutationBudget] times before advancing round-robin to the next.
    if (state.currentParent == null || state.remainingBudget <= 0) {
      state.currentParent = state.corpus[state.parentCursor % state.corpus.size]
      state.parentCursor += 1
      state.remainingBudget = mutationBudget
    }

    val mutant = state.currentParent!!.mutate(state.mutationRng, mutationCount)
    state.remainingBudget -= 1

    // Bind a fresh base to the mutated stream, so base.rand is the next recording.
    val next = FestScheduler(base.nextIteration(mutant.toRandom()), mutationCount, mutationBudget)
    next.coverage = coverage
    next.exploration = state
    return next
  }

  companion object {
    const val DEFAULT_MUTATION_COUNT = 5
    const val DEFAULT_MUTATION_BUDGET = 16
  }
}

class FestExploration {
  val corpus: MutableList<FestRecording> = mutableListOf()
  var lastCoverage: Int = 0
  var currentParent: FestRecording? = null
  var remainingBudget: Int = 0
  var parentCursor: Int = 0
  val mutationRng: Random = Random()
}
