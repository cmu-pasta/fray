package org.pastalab.fray.idea.debugger

import com.intellij.util.ui.UIUtil
import com.intellij.xdebugger.XDebugSession
import org.pastalab.fray.idea.`object`.ThreadExecutionContext
import java.util.concurrent.CountDownLatch
import org.pastalab.fray.idea.ui.FrayDebugPanel
import org.pastalab.fray.rmi.RemoteScheduler
import org.pastalab.fray.rmi.ThreadInfo

class FrayDebuggerScheduler(val schedulerPanel: FrayDebugPanel, val debugSession: XDebugSession) :
    RemoteScheduler {

  override fun scheduleNextOperation(threads: List<ThreadInfo>): Int {
    val cdl = CountDownLatch(1)
    var selected = 0
    schedulerPanel.schedule(
        threads.map { ThreadExecutionContext(it, debugSession.project) }.toList(),
        {
          selected = threads.indexOf(it.threadInfo)
          cdl.countDown()
        })
    UIUtil.invokeLaterIfNeeded {
      val content = debugSession.ui.findContent(FrayDebugPanel.CONTENT_ID)
      debugSession.ui.selectAndFocus(content, false, false)
    }
    cdl.await()
    return selected
  }
}
