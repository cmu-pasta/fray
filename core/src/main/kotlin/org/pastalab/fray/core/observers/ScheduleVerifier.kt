package org.pastalab.fray.core.observers

import java.io.File
import kotlinx.serialization.json.Json
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.rmi.ScheduleObserver
import org.pastalab.fray.rmi.TestStatusObserver

class ScheduleVerifier(val schedules: List<ScheduleRecording>) :
    ScheduleObserver<ThreadContext>, TestStatusObserver {
  constructor(
      path: String
  ) : this(Json.decodeFromString<List<ScheduleRecording>>(File(path).readText()))

  var index = 0

  override fun onExecutionStart() {
    index = 0
  }

  override fun onNewSchedule(allThreads: Collection<ThreadContext>, scheduled: ThreadContext) {
    if (index >= schedules.size) {
      return
    }
    val recording = schedules[index]
    val scheduledIndex = scheduled.index
    val enabled = allThreads.map { it.index }.toList()
    val operation = scheduled.pendingOperation.javaClass.name
    if (recording.scheduled != scheduledIndex) {
      throw IllegalStateException(
          "Scheduled index mismatch: expected ${recording.scheduled}, got $scheduledIndex")
    }
    if (recording.enabled != enabled) {
      throw IllegalStateException(
          "Enabled schedules mismatch: expected ${recording.enabled}, got $enabled")
    }
    if (recording.operation != operation) {
      throw IllegalStateException(
          "Operation mismatch: expected ${recording.operation}, got $operation")
    }
    index++
  }

  override fun onContextSwitch(current: ThreadContext, next: ThreadContext) {}

  override fun onExecutionDone(bugFound: Throwable?) {}

  override fun saveToReportFolder(path: String) {}
}
