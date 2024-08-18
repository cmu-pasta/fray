package org.pastalab.fray.core.observers

import org.pastalab.fray.core.ThreadContext

interface ScheduleObserver {
  fun onExecutionStart()

  fun onNewSchedule(enabledSchedules: List<ThreadContext>, scheduled: ThreadContext)

  fun onExecutionDone()

  fun saveToReportFolder(path: String)
}
