package org.pastalab.fray.idea.debugger

import com.intellij.util.ui.UIUtil
import com.intellij.xdebugger.XDebugSession
import java.util.concurrent.CountDownLatch
import org.pastalab.fray.idea.objects.ThreadExecutionContext
import org.pastalab.fray.idea.ui.FrayDebugPanel
import org.pastalab.fray.rmi.RemoteScheduler
import org.pastalab.fray.rmi.ScheduleObserver
import org.pastalab.fray.rmi.ThreadInfo
import org.pastalab.fray.rmi.ThreadState

class FrayDebuggerScheduler(
    val schedulerPanel: FrayDebugPanel,
    val debugSession: XDebugSession,
    val replayMode: Boolean
) : RemoteScheduler, ScheduleObserver<ThreadExecutionContext> {

  override fun scheduleNextOperation(threads: List<ThreadInfo>): Int {
    // Schedule is only enabled in debug mode.
    if (replayMode) return -1
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

  override fun onExecutionStart() {}

  override fun onNewSchedule(
      allThreads: List<ThreadExecutionContext>,
      scheduled: ThreadExecutionContext
  ) {
    if (!replayMode) return
    if (allThreads.count { it.threadInfo.state == ThreadState.Runnable } <= 1) return
    val cdl = CountDownLatch(1)
    schedulerPanel.schedule(allThreads, { cdl.countDown() })
    UIUtil.invokeLaterIfNeeded {
      val content = debugSession.ui.findContent(FrayDebugPanel.CONTENT_ID)
      debugSession.ui.selectAndFocus(content, false, false)
    }
    cdl.await()
  }

  override fun onExecutionDone(bugFound: Throwable?) {}

  override fun saveToReportFolder(path: String) {}
}
