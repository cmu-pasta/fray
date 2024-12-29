package org.pastalab.fray.idea

import org.pastalab.fray.rmi.RemoteScheduler

class FrayDebuggerScheduler: RemoteScheduler {
  override fun scheduleNextOperation(threads: List<Long>): Int {
    return 0
  }
}
