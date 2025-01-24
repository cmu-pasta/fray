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
    var operation = scheduled.pendingOperation.toString()
    var count = 0
    for (st in Thread.currentThread().stackTrace.drop(1)) {
      if (st.className.startsWith("org.pastalab.fray")) {
        continue
      }
      operation += "@${st.className}.${st.methodName},"
      count += 1
      if (count == 3) break
    }
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
