package org.pastalab.fray.core.observers

import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.rmi.ScheduleObserver
import org.pastalab.fray.rmi.ThreadInfo

class ScheduleSaver(val config: Configuration) : ScheduleObserver<ThreadInfo> {
  override fun onExecutionStart() {}

  override fun onNewSchedule(enabledSchedules: List<ThreadInfo>, scheduled: ThreadInfo) {
    config.saveToReportFolder(0)
  }

  override fun onExecutionDone(bugFound: Boolean) {}

  override fun saveToReportFolder(path: String) {}
}
