package org.anonlab.fray.core.observers

import java.io.File
import kotlinx.serialization.json.Json
import org.anonlab.fray.rmi.ScheduleObserver
import org.anonlab.fray.rmi.ThreadInfo

class ScheduleVerifier(val schedules: List<ScheduleRecording>) : ScheduleObserver<ThreadInfo> {
  constructor(
      path: String
  ) : this(Json.decodeFromString<List<ScheduleRecording>>(File(path).readText()))

  var index = 0

  override fun onExecutionStart() {
    index = 0
  }

  override fun onNewSchedule(enabledSchedules: List<ThreadInfo>, scheduled: ThreadInfo) {
    if (index >= schedules.size) {
      return
    }
    val recording = schedules[index]
    val scheduledIndex = scheduled.threadIndex
    val enabled = enabledSchedules.map { it.threadIndex }.toList()
    var operation = ""
    var count = 0
    for (st in scheduled.stackTraces) {
      if (st.className.startsWith("org.anonlab.fray")) {
        continue
      }
      operation += "@${st.className}.${st.methodName},"
      count += 1
      if (count == 3) break
    }
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

  override fun onExecutionDone() {}

  override fun saveToReportFolder(path: String) {}
}
