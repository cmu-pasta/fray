package org.pastalab.fray.rmi

import java.rmi.Remote
import java.rmi.RemoteException

interface ScheduleObserver<T> : Remote {

  @Throws(RemoteException::class) fun onNewSchedule(allThreads: List<T>, scheduled: T)

  companion object {
    const val NAME = "ScheduleObserver"
  }
}
