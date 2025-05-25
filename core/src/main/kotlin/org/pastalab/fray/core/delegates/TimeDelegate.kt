package org.pastalab.fray.core.delegates

import java.time.Instant
import org.pastalab.fray.core.controllers.TimeController
import org.pastalab.fray.runtime.TimeDelegate

class TimeDelegate(val controller: TimeController, val synchronizer: DelegateSynchronizer) :
    TimeDelegate() {
  override fun onNanoTime(): Long =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          { controller.nanoTime() },
          { System.nanoTime() },
      )

  override fun onCurrentTimeMillis(): Long =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          { controller.currentTimeMillis() },
          { System.currentTimeMillis() },
      )

  override fun onInstantNow(): Instant =
      synchronizer.runInFrayDoneWithOriginBlockAndNoSkip(
          { controller.instantNow() },
          { Instant.now() },
      )
}
