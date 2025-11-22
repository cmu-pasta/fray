package org.pastalab.fray.mcp

import org.pastalab.fray.rmi.ThreadInfo

interface ScheduleResultListener {
  fun scheduled(thread: ThreadInfo)
}
