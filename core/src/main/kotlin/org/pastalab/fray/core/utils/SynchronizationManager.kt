package org.pastalab.fray.core.utils

class SynchronizationManager {
  val synchronizationPoints = mutableMapOf<Int, Sync>()

  fun removeWait(obj: Any) {
    val id = System.identityHashCode(obj)
    synchronizationPoints.remove(id)
  }

  fun createWait(obj: Any, times: Int) {
    val id = System.identityHashCode(obj)
    Utils.verifyOrReport(!synchronizationPoints.contains(id))
    synchronizationPoints[id] = Sync(times)
  }

  fun wait(obj: Any) {
    val id = System.identityHashCode(obj)
    synchronizationPoints[id]?.block()
    synchronizationPoints.remove(id)
  }

  fun signal(obj: Any) {
    val id = System.identityHashCode(obj)
    synchronizationPoints[id]?.unblock()
  }
}
