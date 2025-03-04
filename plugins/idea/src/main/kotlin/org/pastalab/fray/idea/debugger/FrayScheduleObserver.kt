package org.pastalab.fray.idea.debugger

import com.intellij.openapi.project.Project
import org.pastalab.fray.idea.objects.ThreadExecutionContext
import org.pastalab.fray.rmi.ScheduleObserver
import org.pastalab.fray.rmi.ThreadInfo

class FrayScheduleObserver(val project: Project) : ScheduleObserver<ThreadInfo> {
  val observers = mutableListOf<ScheduleObserver<ThreadExecutionContext>>()

  override fun onExecutionStart() {
    observers.forEach { it.onExecutionStart() }
  }

  override fun onNewSchedule(enabledSchedules: List<ThreadInfo>, scheduled: ThreadInfo) {
    observers.forEach {
      it.onNewSchedule(
          enabledSchedules.map { ThreadExecutionContext(it, project) }.toList(),
          ThreadExecutionContext(scheduled, project))
    }
  }

  override fun onExecutionDone() {
    observers.forEach { it.onExecutionDone() }
  }

  override fun saveToReportFolder(path: String) {
    observers.forEach { it.saveToReportFolder(path) }
  }
}
