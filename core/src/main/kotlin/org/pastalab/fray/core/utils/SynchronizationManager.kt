package org.pastalab.fray.core.utils

import org.pastalab.fray.core.utils.Utils.verifyOrReport

class SynchronizationManager {
  val synchronizationPoints = mutableMapOf<Int, Sync>()

  fun removeWait(obj: Any) {
    val id = System.identityHashCode(obj)
    synchronizationPoints.remove(id)
  }

  fun createWait(obj: Any, times: Int) {
    val id = System.identityHashCode(obj)
    verifyOrReport { !synchronizationPoints.contains(id) }
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
