package org.pastalab.fray.core.observers

import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.writeText
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.RacingOperation

class ResourceOrderingCoverage(private val minThreads: Int = 2) : TimelineCoverage {
  private val coveredTimelines = mutableSetOf<Int>()
  private var tracker = ResourceTracker()

  override fun onExecutionStart() {
    tracker = ResourceTracker()
  }

  override fun onNewSchedule(allThreads: Collection<ThreadContext>, scheduled: ThreadContext) {
    val operation = scheduled.pendingOperation
    if (operation is RacingOperation) {
      tracker.recordAccess(operation.resource, scheduled.index)
    }
  }

  override fun onContextSwitch(current: ThreadContext, next: ThreadContext) {}

  override fun onExecutionDone(bugFound: Throwable?) {
    coveredTimelines.add(tracker.fingerprint(minThreads))
  }

  override fun onReportError(throwable: Throwable) {}

  override fun saveToReportFolder(path: Path) {
    (path / "timeline_coverage.txt").writeText("covered_timelines: ${coveredTimelines.size}")
  }

  override fun getCoverage(): Int = coveredTimelines.size
}

// Tracks per-resource thread access patterns across a single execution.
//
// For each shared resource, we record:
// 1. Which threads accessed it (threadBitset)
// 2. Which directed thread transitions occurred on it (transitionSet)
//    e.g., "thread 2 accessed resource X immediately after thread 1" is the transition (1 -> 2)
//
// At the end of an execution, we combine these into a fingerprint. Two executions with the
// same fingerprint exhibited identical thread-handoff patterns on all contended resources.
class ResourceTracker {
  private val resourceIndex = HashMap<Int, Int>(256)
  private val firstThread = HashMap<Int, Int>(256)
  private val lastThread = HashMap<Int, Int>(256)
  // Per stable ID: bitmask of all threads that accessed the resource (bit i = thread i)
  private val threadBitset = HashMap<Int, Long>(256)
  // Per stable ID: encodes the set of directed transitions (prev_thread -> current_thread)
  private val transitionSet = HashMap<Int, Long>(256)

  fun recordAccess(resource: Int, threadIndex: Int) {
    val prev = lastThread.put(resource, threadIndex)
    if (prev == null) {
      firstThread[resource] = threadIndex
      return
    }
    // Same thread accessing again — no transition
    if (prev == threadIndex) return

    // First time two different threads access this resource — assign a stable ID
    val stableId = resourceIndex.getOrPut(resource) { resourceIndex.size }
    // Update the set of threads that have accessed this resource
    threadBitset[stableId] =
        (threadBitset[stableId] ?: ((1L shl firstThread[resource]!!) or (1L shl threadIndex))) or
            (1L shl threadIndex)

    // Record the directed transition (prev -> threadIndex).
    // We encode each transition as a unique bit position: prev * 8 + threadIndex.
    // This supports up to 8 threads exactly (8*8 = 64 possible transitions fit in a Long).
    val transitionBit = prev * 8 + threadIndex
    if (transitionBit < 64) {
      // Fast path: set the bit corresponding to this transition
      transitionSet[stableId] = (transitionSet[stableId] ?: 0L) or (1L shl transitionBit)
    } else {
      // Fallback for >8 threads: XOR a hash of the transition pair into the value.
      // This loses the ability to distinguish individual transitions but still produces
      // different fingerprints for different transition sets (with high probability).
      val existing = transitionSet[stableId] ?: 0L
      transitionSet[stableId] = existing xor (prev.toLong() * 67 + threadIndex.toLong())
    }
  }

  // Produce a single hash summarizing the thread-handoff patterns of this execution.
  // Only resources accessed by at least [minThreads] threads contribute to the fingerprint.
  fun fingerprint(minThreads: Int): Int {
    var hash = 0
    for (stableId in 0 until resourceIndex.size) {
      val bitset = threadBitset[stableId] ?: continue
      if (java.lang.Long.bitCount(bitset) < minThreads) continue
      val transitions = transitionSet.getOrDefault(stableId, 0L)
      hash = hash * 31 + stableId
      hash = hash * 31 + bitset.hashCode()
      hash = hash * 31 + transitions.hashCode()
    }
    return hash
  }
}
