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

  override fun onNewSchedule(allThreads: List<ThreadInfo>, scheduled: ThreadInfo) {
    // Disable this callback for now to improve the performance.
  }

  override fun onExecutionDone(bugFound: Throwable?) {
    observers.forEach { it.onExecutionDone(bugFound) }
  }

  override fun saveToReportFolder(path: String) {
    observers.forEach { it.saveToReportFolder(path) }
  }
}
