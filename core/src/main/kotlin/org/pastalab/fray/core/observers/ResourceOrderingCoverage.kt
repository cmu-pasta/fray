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

class ResourceTracker {
  private val resourceIndex = HashMap<Int, Int>(256)
  private val firstThread = HashMap<Int, Int>(256)
  private val lastThread = HashMap<Int, Int>(256)
  private val threadBitset = HashMap<Int, Long>(256)
  private val transitionSet = HashMap<Int, Long>(256)

  fun recordAccess(resource: Int, threadIndex: Int) {
    val prev = lastThread.put(resource, threadIndex)
    if (prev == null) {
      firstThread[resource] = threadIndex
      return
    }
    if (prev == threadIndex) return

    val stableId = resourceIndex.getOrPut(resource) { resourceIndex.size }
//    val stableId = resource
    threadBitset[stableId] =
        (threadBitset[stableId] ?: ((1L shl firstThread[resource]!!) or (1L shl threadIndex))) or
            (1L shl threadIndex)
    val transitionBit = prev * 8 + threadIndex
    if (transitionBit < 64) {
      transitionSet[stableId] = (transitionSet[stableId] ?: 0L) or (1L shl transitionBit)
    } else {
      val existing = transitionSet[stableId] ?: 0L
      transitionSet[stableId] = existing xor (prev.toLong() * 67 + threadIndex.toLong())
    }
  }

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
