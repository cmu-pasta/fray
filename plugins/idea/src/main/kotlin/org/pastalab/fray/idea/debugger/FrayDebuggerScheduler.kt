package org.pastalab.fray.idea.debugger

import com.intellij.util.ui.UIUtil
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.impl.XDebugSessionImpl
import java.util.concurrent.CountDownLatch
import org.pastalab.fray.idea.objects.ThreadExecutionContext
import org.pastalab.fray.idea.ui.FrayDebugPanel
import org.pastalab.fray.rmi.RemoteScheduler
import org.pastalab.fray.rmi.ThreadInfo

class FrayDebuggerScheduler(
    val schedulerPanel: FrayDebugPanel,
    val debugSession: XDebugSession,
    val replayMode: Boolean,
) : RemoteScheduler {

  override fun scheduleNextOperation(threads: List<ThreadInfo>, selectedThread: ThreadInfo?): Int {
    // Schedule is only enabled in debug mode.
    assert(replayMode || selectedThread == null)
    val cdl = CountDownLatch(1)
    var selected = 0
    val threadContexts = threads.map { ThreadExecutionContext(it, debugSession.project) }.toList()
    schedulerPanel.schedule(
        threadContexts,
        selectedThread?.let { ThreadExecutionContext(it, debugSession.project) },
        {
          selected = threads.indexOf(it)
          cdl.countDown()
        },
    )
    UIUtil.invokeLaterIfNeeded {
      val session = debugSession
      if (session is XDebugSessionImpl) {
        session.runWhenUiReady {
          val content = it.findContent(FrayDebugPanel.CONTENT_ID)
          it.selectAndFocus(content!!, false, false)
        }
      }
    }
    cdl.await()
    val nextThreadIndex =
        if (selectedThread != null) {
          threads.indexOf(selectedThread)
        } else {
          selected
        }
    schedulerPanel.threadTimelinePanel.onNewSchedule(
        threadContexts,
        threadContexts[nextThreadIndex],
    )
    return nextThreadIndex
  }
}
