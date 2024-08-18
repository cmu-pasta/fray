package org.pastalab.fray.core.observers

import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.pastalab.fray.core.ThreadContext

class ScheduleRecorder : ScheduleObserver {
  val recordings = mutableListOf<ScheduleRecording>()

  override fun onExecutionStart() {
    recordings.clear()
  }

  override fun onNewSchedule(enabledSchedules: List<ThreadContext>, scheduled: ThreadContext) {
    val operation = scheduled.pendingOperation.javaClass.name
    val enabled = enabledSchedules.map { it.index }.toList()
    val scheduledIndex = scheduled.index
    val recording = ScheduleRecording(scheduledIndex, enabled, operation)
    recordings.add(recording)
  }

  override fun onExecutionDone() {}

  override fun saveToReportFolder(path: String) {
    File("$path/recording.json").writeText(Json.encodeToString(recordings))
  }
}
