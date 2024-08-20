package org.pastalab.fray.core.observers

import org.pastalab.fray.core.ThreadContext

class ScheduleVerifier(val schedules: List<ScheduleRecording>) : ScheduleObserver {
  var index = 0

  override fun onExecutionStart() {
    index = 0
  }

  override fun onNewSchedule(enabledSchedules: List<ThreadContext>, scheduled: ThreadContext) {
    if (index >= schedules.size) {
      return
    }
    val recording = schedules[index]
    val scheduledIndex = scheduled.index
    val enabled = enabledSchedules.map { it.index }.toList()
    val operation = scheduled.pendingOperation.toString()
    if (recording.scheduled != scheduledIndex) {
//      throw IllegalStateException(
//          "Scheduled index mismatch: expected ${recording.scheduled}, got $scheduledIndex")
    }
    if (recording.enabled != enabled) {
//      throw IllegalStateException(
//          "Enabled schedules mismatch: expected ${recording.enabled}, got $enabled")
    }
    if (recording.operation != operation) {
//      throw IllegalStateException(
//          "Operation mismatch: expected ${recording.operation}, got $operation")
    }
    index++
  }

  override fun onExecutionDone() {}

  override fun saveToReportFolder(path: String) {}
}
