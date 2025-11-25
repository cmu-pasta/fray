package org.pastalab.fray.core.debugger

import java.rmi.ConnectException
import java.rmi.NotBoundException
import java.rmi.RemoteException
import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import org.pastalab.fray.rmi.Constant
import org.pastalab.fray.rmi.RemoteScheduler
import org.pastalab.fray.rmi.TestStatusObserver

object DebuggerRegistry {
  val registry: Registry = LocateRegistry.getRegistry("localhost", Constant.REGISTRY_PORT)
  private const val RETRY_DELAY_MS = 500L

  fun getRemoteScheduler(): RemoteScheduler {
    return getRegistryWithRetries(RemoteScheduler.NAME)
  }

  fun getRemoteScheduleObserver(): TestStatusObserver {
    return getRegistryWithRetries(TestStatusObserver.NAME)
  }

  private fun <T> getRegistryWithRetries(name: String): T {
    while (true) {
      try {
        return registry.lookup(name) as T
      } catch (e: NotBoundException) {
        // scheduler not bound yet, retry
      } catch (e: RemoteException) {
        // registry not reachable yet, retry
      } catch (e: ConnectException) {
        // registry not reachable yet, retry
      } catch (e: ClassCastException) {
        // unexpected type bound under the name -> propagate
        throw e
      }
      try {
        Thread.sleep(RETRY_DELAY_MS)
      } catch (ie: InterruptedException) {
        Thread.currentThread().interrupt()
        throw RuntimeException("Interrupted while waiting for RMI registry or RemoteScheduler", ie)
      }
    }
  }
}
