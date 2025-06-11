package org.pastalab.fray.rmi

import java.rmi.Remote
import java.rmi.RemoteException

interface ScheduleObserver<T> : Remote {

  @Throws(RemoteException::class) fun onNewSchedule(allThreads: Collection<T>, scheduled: T)

  @Throws(RemoteException::class) fun onContextSwitch(current: T, next: T)

  companion object {
    const val NAME = "ScheduleObserver"
  }
}
