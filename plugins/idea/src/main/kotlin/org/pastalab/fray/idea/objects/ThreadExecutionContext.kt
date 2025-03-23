package org.pastalab.fray.idea.objects

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import java.awt.Color
import javax.swing.Icon
import org.pastalab.fray.idea.getPsiFile
import org.pastalab.fray.idea.ui.Colors.THREAD_DISABLED_COLOR
import org.pastalab.fray.idea.ui.Colors.THREAD_ENABLED_COLOR
import org.pastalab.fray.rmi.ThreadInfo
import org.pastalab.fray.rmi.ThreadState

class ThreadExecutionContext(val threadInfo: ThreadInfo, project: Project) {
  var executingFrame: StackTraceElement? = null

  init {
    for (stack in threadInfo.stackTraces) {
      executingFrame = stack
      if (stack.getPsiFile(project) != null) break
    }
  }

  fun threadStateIcon(): Icon {
    return if (threadInfo.state == ThreadState.Runnable) AllIcons.Debugger.ThreadRunning
    else AllIcons.Debugger.ThreadFrozen
  }

  fun threadStateColor(): Color {
    return if (threadInfo.state == ThreadState.Runnable) THREAD_ENABLED_COLOR
    else THREAD_DISABLED_COLOR
  }

  override fun toString(): String {
    return "${threadInfo.threadName} (${threadInfo.state})"
  }
}
