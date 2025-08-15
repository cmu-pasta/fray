package org.pastalab.fray.core.debugger

import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import org.pastalab.fray.rmi.Constant
import org.pastalab.fray.rmi.RemoteScheduler
import org.pastalab.fray.rmi.TestStatusObserver

object DebuggerRegistry {
  val registry: Registry = LocateRegistry.getRegistry("localhost", Constant.REGISTRY_PORT)

  fun getRemoteScheduler(): RemoteScheduler {
    return registry.lookup(RemoteScheduler.NAME) as RemoteScheduler
  }

  fun getRemoteScheduleObserver(): TestStatusObserver {
    return registry.lookup(TestStatusObserver.NAME) as TestStatusObserver
  }
}
