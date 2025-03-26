package mcp

import org.pastalab.fray.rmi.ThreadInfo

interface SchedulerDelegate {
  fun scheduled(thread: ThreadInfo)
}
