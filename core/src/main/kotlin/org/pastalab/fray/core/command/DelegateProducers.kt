package org.pastalab.fray.core.command

import org.pastalab.fray.core.RunContext
import org.pastalab.fray.core.controllers.ProactiveNetworkController
import org.pastalab.fray.core.controllers.TimeController
import org.pastalab.fray.core.delegates.DelegateSynchronizer
import org.pastalab.fray.core.delegates.ProactiveNetworkDelegate
import org.pastalab.fray.runtime.NetworkDelegate
import org.pastalab.fray.runtime.TimeDelegate

enum class NetworkDelegateType {
  PROACTIVE,
  NONE,
}

enum class TimeDelegateType {
  MOCK,
  NONE,
}

class DelegateProducers(val configuration: Configuration) {
  fun produceNetworkDelegate(
      runContext: RunContext,
      synchronizer: DelegateSynchronizer
  ): NetworkDelegate {
    return when (configuration.networkDelegateType) {
      NetworkDelegateType.PROACTIVE ->
          ProactiveNetworkDelegate(ProactiveNetworkController(runContext), synchronizer)
      else -> NetworkDelegate()
    }
  }

  fun produceTimeDelegate(
      runContext: RunContext,
      synchronizer: DelegateSynchronizer
  ): TimeDelegate {
    return when (configuration.timeDelegateType) {
      TimeDelegateType.MOCK ->
          org.pastalab.fray.core.delegates.TimeDelegate(TimeController(runContext), synchronizer)
      else -> TimeDelegate()
    }
  }
}
