package org.pastalab.fray.rmi

import java.rmi.Remote
import java.rmi.RemoteException

interface ScheduleObserver<T> : Remote {
  @Throws(RemoteException::class) fun onExecutionStart()

  @Throws(RemoteException::class) fun onNewSchedule(allThreads: List<T>, scheduled: T)

  @Throws(RemoteException::class) fun onExecutionDone(bugFound: Throwable?)

  @Throws(RemoteException::class) fun saveToReportFolder(path: String)

  companion object {
    const val NAME = "ScheduleObserver"
  }
}
