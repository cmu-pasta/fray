package org.anonlab.fray.core.observers

import org.anonlab.fray.core.command.Configuration
import org.anonlab.fray.rmi.ScheduleObserver
import org.anonlab.fray.rmi.ThreadInfo

class ScheduleSaver(val config: Configuration) : ScheduleObserver<ThreadInfo> {
  override fun onExecutionStart() {}

  override fun onNewSchedule(enabledSchedules: List<ThreadInfo>, scheduled: ThreadInfo) {
    config.saveToReportFolder(0)
  }

  override fun onExecutionDone() {}

  override fun saveToReportFolder(path: String) {}
}
