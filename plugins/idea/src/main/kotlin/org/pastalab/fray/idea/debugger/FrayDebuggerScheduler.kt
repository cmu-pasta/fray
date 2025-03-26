package org.pastalab.fray.idea.debugger

import com.intellij.util.ui.UIUtil
import com.intellij.xdebugger.XDebugSession
import java.util.concurrent.CountDownLatch
import org.pastalab.fray.idea.objects.ThreadExecutionContext
import org.pastalab.fray.idea.ui.FrayDebugPanel
import org.pastalab.fray.rmi.RemoteScheduler
import org.pastalab.fray.rmi.ThreadInfo

class FrayDebuggerScheduler(
    val schedulerPanel: FrayDebugPanel,
    val debugSession: XDebugSession,
    val replayMode: Boolean
) : RemoteScheduler {

  override fun scheduleNextOperation(threads: List<ThreadInfo>, scheduledThread: ThreadInfo?): Int {
    // Schedule is only enabled in debug mode.
    assert(replayMode || scheduledThread == null)
    val cdl = CountDownLatch(1)
    var selected = 0
    schedulerPanel.schedule(
        threads.map { ThreadExecutionContext(it, debugSession.project) }.toList(),
        scheduledThread?.let { ThreadExecutionContext(it, debugSession.project) },
        {
          selected = threads.indexOf(it.threadInfo)
          cdl.countDown()
        })
    UIUtil.invokeLaterIfNeeded {
      val content = debugSession.ui.findContent(FrayDebugPanel.CONTENT_ID)
      debugSession.ui.selectAndFocus(content, false, false)
    }
    cdl.await()
    return if (scheduledThread != null) {
      threads.indexOf(scheduledThread)
    } else {
      selected
    }
  }
}
