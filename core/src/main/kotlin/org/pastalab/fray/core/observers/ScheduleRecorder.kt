package org.pastalab.fray.core.observers

import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.pastalab.fray.rmi.ScheduleObserver
import org.pastalab.fray.rmi.ThreadInfo

class ScheduleRecorder : ScheduleObserver<ThreadInfo> {
  val recordings = mutableListOf<ScheduleRecording>()

  override fun onExecutionStart() {
    recordings.clear()
  }

  override fun onNewSchedule(allThreads: List<ThreadInfo>, scheduled: ThreadInfo) {
    var count = 0
    var operation = ""
    for (st in scheduled.stackTraces) {
      if (st.className.startsWith("org.pastalab.fray")) {
        continue
      }
      operation += "@${st.className}.${st.methodName},"
      count += 1
      if (count == 3) break
    }
    val enabled = allThreads.map { it.threadIndex }.toList()
    val scheduledIndex = scheduled.threadIndex
    val recording = ScheduleRecording(scheduledIndex, enabled, operation)
    recordings.add(recording)
  }

  override fun onExecutionDone(bugFound: Boolean) {}

  override fun saveToReportFolder(path: String) {
    File("$path/recording.json").writeText(Json.encodeToString(recordings))
  }
}
