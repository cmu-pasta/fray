package org.pastalab.fray.core.command

import org.pastalab.fray.core.RunContext
import org.pastalab.fray.core.controllers.ProactiveNetworkController
import org.pastalab.fray.core.controllers.ReactiveNetworkController
import org.pastalab.fray.core.delegates.DelegateSynchronizer
import org.pastalab.fray.core.delegates.ProactiveNetworkDelegate
import org.pastalab.fray.core.delegates.ReactiveNetworkDelegate
import org.pastalab.fray.runtime.NetworkDelegate

enum class NetworkDelegateType {
  PROACTIVE {
    override fun produce(context: RunContext, synchronizer: DelegateSynchronizer): NetworkDelegate {
      return ProactiveNetworkDelegate(ProactiveNetworkController(context), synchronizer)
    }
  },
  REACTIVE {
    override fun produce(context: RunContext, synchronizer: DelegateSynchronizer): NetworkDelegate {
      return ReactiveNetworkDelegate(ReactiveNetworkController(context), synchronizer)
    }
  },
  NONE {
    override fun produce(context: RunContext, synchronizer: DelegateSynchronizer): NetworkDelegate {
      return NetworkDelegate()
    }
  };

  abstract fun produce(
      context: RunContext,
      synchronizer: DelegateSynchronizer,
  ): NetworkDelegate
}

enum class SystemTimeDelegateType {
  MOCK,
  NONE,
}
