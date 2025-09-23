package org.pastalab.fray.core.observers

import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.rmi.ScheduleObserver
import org.pastalab.fray.rmi.TestStatusObserver

class ScheduleRecorder : ScheduleObserver<ThreadContext>, TestStatusObserver {
  val recordings = mutableListOf<ScheduleRecording>()

  override fun onExecutionStart() {
    recordings.clear()
  }

  override fun onReportError(throwable: Throwable) {
    // No-op for schedule recorder
  }

  override fun onNewSchedule(allThreads: Collection<ThreadContext>, scheduled: ThreadContext) {
    val enabled = allThreads.map { it.index }.toList()
    val scheduledIndex = scheduled.index
    val recording =
        ScheduleRecording(scheduledIndex, enabled, scheduled.pendingOperation.javaClass.name)
    recordings.add(recording)
  }

  override fun onContextSwitch(current: ThreadContext, next: ThreadContext) {}

  override fun onExecutionDone(bugFound: Throwable?) {}

  override fun saveToReportFolder(path: String) {
    File("$path/recording.json").writeText(Json.encodeToString(recordings))
  }
}
