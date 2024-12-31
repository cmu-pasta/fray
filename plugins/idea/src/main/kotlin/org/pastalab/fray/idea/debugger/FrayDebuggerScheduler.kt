package org.pastalab.fray.idea.debugger

import com.intellij.xdebugger.XDebugSession
import java.util.concurrent.CountDownLatch
import org.pastalab.fray.idea.ui.SchedulerPanel
import org.pastalab.fray.rmi.RemoteScheduler

class FrayDebuggerScheduler(val schedulerPanel: SchedulerPanel) : RemoteScheduler {
  var currentDebuggSession: XDebugSession? = null

  override fun scheduleNextOperation(threads: List<Long>): Int {
    currentDebuggSession?.pause()

    val cdl = CountDownLatch(1)
    var selected = 0
    schedulerPanel.schedule(
        threads,
        {
          selected = threads.indexOf(it)
          cdl.countDown()
        })
    cdl.await()
    currentDebuggSession?.resume()
    return selected
  }
}
