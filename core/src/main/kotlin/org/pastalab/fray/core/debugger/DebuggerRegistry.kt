package org.pastalab.fray.core.debugger

import java.rmi.registry.LocateRegistry
import org.pastalab.fray.rmi.Constant
import org.pastalab.fray.rmi.RemoteScheduler
import org.pastalab.fray.rmi.ScheduleObserver
import org.pastalab.fray.rmi.ThreadInfo

object DebuggerRegistry {
  val registry = LocateRegistry.getRegistry("localhost", Constant.REGISTRY_PORT)

  fun getRemoteScheduler(): RemoteScheduler {
    return registry.lookup(RemoteScheduler.NAME) as RemoteScheduler
  }

  fun getRemoteScheduleObserver(): ScheduleObserver<ThreadInfo> {
    return registry.lookup(ScheduleObserver.NAME) as ScheduleObserver<ThreadInfo>
  }
}
