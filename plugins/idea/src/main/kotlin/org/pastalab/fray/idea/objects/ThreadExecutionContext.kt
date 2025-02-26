package org.pastalab.fray.idea.objects

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.ClassUtil
import com.intellij.ui.JBColor
import java.awt.Color
import org.pastalab.fray.rmi.ThreadInfo

class ThreadExecutionContext(val threadInfo: ThreadInfo, project: Project) {
  var executingLine = -1
  var document: Document? = null
  var virtualFile: VirtualFile? = null
  var executingFrame: StackTraceElement? = null

  init {
    for (stack in getStackTraces()) {
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
        executingLine = stack.lineNumber
        document = psiFile.fileDocument
        true
      })
          break
    }
  }

  fun getStackTraces(): List<StackTraceElement> {
    if (threadInfo.stackTraces.isEmpty()) {
      return listOf(StackTraceElement("N/A", "ThreadEnds", "ThreadEnds", -1))
    } else {
      return threadInfo.stackTraces
    }
  }

  override fun toString(): String {
    return "${threadInfo.threadName} (${threadInfo.state})"
  }

  private fun getThreadColor(): JBColor {
    // Generate a consistent color based on thread index
    // Using HSB color model to create visually distinct colors
    val hue = ((threadInfo.index * 0.618033988749895) % 1).toFloat()

    // Create light and dark theme variants of the same hue
    val lightModeColor = Color.getHSBColor(hue, 0.65f, 0.85f)
    val darkModeColor = Color.getHSBColor(hue, 0.65f, 0.75f)

    // Return a JBColor that adapts to the current theme
    return JBColor(lightModeColor, darkModeColor)
  }
}
