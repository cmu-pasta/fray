package org.pastalab.fray.idea.debugger

import com.intellij.util.ui.UIUtil
import com.intellij.xdebugger.XDebugSession
import java.util.concurrent.CountDownLatch
import org.pastalab.fray.idea.ui.SchedulerPanel
import org.pastalab.fray.rmi.RemoteScheduler
import org.pastalab.fray.rmi.ThreadInfo

class FrayDebuggerScheduler(val schedulerPanel: SchedulerPanel, val debugSession: XDebugSession) :
    RemoteScheduler {

  override fun scheduleNextOperation(threads: List<ThreadInfo>): Int {
    val cdl = CountDownLatch(1)
    var selected = 0
    schedulerPanel.schedule(
        threads,
        {
          selected = threads.indexOf(it)
          cdl.countDown()
        })
    UIUtil.invokeLaterIfNeeded {
      val content = debugSession.ui.findContent(SchedulerPanel.CONTENT_ID)
      debugSession.ui.selectAndFocus(content, false, false)
    }
    cdl.await()
    return selected
  }
}
