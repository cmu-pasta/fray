package org.anonlab.fray.core.debugger

import java.rmi.registry.LocateRegistry
import org.anonlab.fray.rmi.Constant
import org.anonlab.fray.rmi.RemoteScheduler
import org.anonlab.fray.rmi.ScheduleObserver
import org.anonlab.fray.rmi.ThreadInfo

object DebuggerRegistry {
  val registry = LocateRegistry.getRegistry("localhost", Constant.REGISTRY_PORT)

  fun getRemoteScheduler(): RemoteScheduler {
    return registry.lookup(RemoteScheduler.NAME) as RemoteScheduler
  }

  fun getRemoteScheduleObserver(): ScheduleObserver<ThreadInfo> {
    return registry.lookup(ScheduleObserver.NAME) as ScheduleObserver<ThreadInfo>
  }
}
