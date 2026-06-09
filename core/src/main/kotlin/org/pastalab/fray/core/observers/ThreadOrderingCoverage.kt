package org.pastalab.fray.core.observers

import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.writeText
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.RacingOperation

class ThreadOrderingCoverage : TimelineCoverage {
  private val coveredTimelines = mutableSetOf<Int>()
  private var currentTimeline = ThreadTimeline()

  override fun onExecutionStart() {
    currentTimeline = ThreadTimeline()
  }

  override fun onNewSchedule(allThreads: Collection<ThreadContext>, scheduled: ThreadContext) {
    val operation = scheduled.pendingOperation
    if (operation is RacingOperation) {
      currentTimeline.recordEvent(scheduled.index, operation)
    }
  }

  override fun onContextSwitch(current: ThreadContext, next: ThreadContext) {}

  override fun onExecutionDone(bugFound: Throwable?) {
    coveredTimelines.add(currentTimeline.abstractState())
  }

  override fun onReportError(throwable: Throwable) {}

  override fun saveToReportFolder(path: Path) {
    (path / "timeline_coverage.txt").writeText("covered_timelines: ${coveredTimelines.size}")
  }

  override fun getCoverage(): Int = coveredTimelines.size
}

class ThreadTimeline {
  private val perThreadEvents = mutableMapOf<Int, MutableSet<String>>()
  private val perThreadPairs = mutableMapOf<Int, MutableSet<Long>>()

  fun recordEvent(threadIndex: Int, operation: RacingOperation) {
    val eventId = "${operation.javaClass.simpleName}:${operation.type}:${operation.stackTraceHash}"

    val events = perThreadEvents.getOrPut(threadIndex) { mutableSetOf() }
    val pairs = perThreadPairs.getOrPut(threadIndex) { mutableSetOf() }
    for (prev in events) {
      pairs.add(pairHash(prev, eventId))
    }
    events.add(eventId)
  }

  fun abstractState(): Int {
    var hash = 0
    for ((threadIndex, pairs) in perThreadPairs.entries.sortedBy { it.key }) {
      hash = hash * 31 + threadIndex
      for (pair in pairs.sorted()) {
        hash = hash * 31 + pair.hashCode()
      }
    }
    for ((threadIndex, events) in perThreadEvents.entries.sortedBy { it.key }) {
      hash = hash * 31 + threadIndex
      for (event in events.sorted()) {
        hash = hash * 31 + event.hashCode()
      }
    }
    return hash
  }

  private fun pairHash(a: String, b: String): Long {
    return a.hashCode().toLong() * 31 + b.hashCode().toLong()
  }
}
