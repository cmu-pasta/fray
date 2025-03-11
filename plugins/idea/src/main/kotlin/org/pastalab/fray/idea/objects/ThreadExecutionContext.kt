package org.pastalab.fray.idea.objects

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.ClassUtil
import java.awt.Color
import javax.swing.Icon
import org.pastalab.fray.idea.ui.Colors.THREAD_DISABLED_COLOR
import org.pastalab.fray.idea.ui.Colors.THREAD_ENABLED_COLOR
import org.pastalab.fray.rmi.ThreadInfo
import org.pastalab.fray.rmi.ThreadState

class ThreadExecutionContext(val threadInfo: ThreadInfo, project: Project) {
  var document: Document? = null
  var virtualFile: VirtualFile? = null
  var executingFrame: StackTraceElement? = null

  init {
    for (stack in threadInfo.stackTraces) {
      executingFrame = stack
      if (stack.lineNumber <= 0) continue
      if (stack.className == "ThreadStartOperation") continue
      if (runReadAction {
        val psiClass =
            ClassUtil.findPsiClassByJVMName(PsiManager.getInstance(project), stack.className)
                ?: return@runReadAction false
        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        val psiFile = psiClass.containingFile
        virtualFile = psiFile.virtualFile
        if (!fileIndex.isInSource(virtualFile!!)) return@runReadAction false
        document = psiFile.fileDocument
        true
      })
          break
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
