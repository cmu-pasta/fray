package org.pastalab.fray.mcp

import org.pastalab.fray.rmi.RemoteScheduler
import org.pastalab.fray.rmi.ThreadInfo

class StandaloneMCPServerScheduler(val replayMode: Boolean) : RemoteScheduler {
  override fun scheduleNextOperation(threads: List<ThreadInfo>, selectedThread: ThreadInfo?): Int {
    return 0
  }
}
