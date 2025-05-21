package org.pastalab.fray.core.delegates

import java.time.Instant
import org.pastalab.fray.core.controllers.TimeController
import org.pastalab.fray.runtime.TimeDelegate

class TimeDelegate(val controller: TimeController, val synchronizer: DelegateSynchronizer) :
    TimeDelegate() {
  override fun onNanoTime(): Long {
    if (synchronizer.checkEntered()) return System.nanoTime()
    val value = controller.nanoTime()
    synchronizer.entered.set(false)
    return value
  }

  override fun onCurrentTimeMillis(): Long {
    if (synchronizer.checkEntered()) return System.currentTimeMillis()
    val value = controller.currentTimeMillis()
    synchronizer.entered.set(false)
    return value
  }

  override fun onInstantNow(): Instant {
    if (synchronizer.checkEntered()) return Instant.now()
    val instant = controller.instantNow()
    synchronizer.entered.set(false)
    return instant
  }
}
