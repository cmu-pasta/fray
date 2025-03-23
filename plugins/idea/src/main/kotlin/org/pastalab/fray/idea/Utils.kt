package org.pastalab.fray.idea

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.ClassUtil

fun StackTraceElement.getPsiFile(project: Project): PsiFile? {
  if (lineNumber <= 0) return null
  if (className == "ThreadStartOperation") return null
  return getPsiFileFromClass(className, project)
}

fun getPsiFileFromClass(className: String, project: Project): PsiFile? {
  return ApplicationManager.getApplication()
      .executeOnPooledThread<PsiFile?> {
        runReadAction {
          val psiClass =
              ClassUtil.findPsiClassByJVMName(PsiManager.getInstance(project), className)
                  ?: return@runReadAction null
          val fileIndex = ProjectRootManager.getInstance(project).fileIndex
          val psiFile = psiClass.containingFile
          if (!fileIndex.isInSourceContent(psiFile.virtualFile)) return@runReadAction null
          psiFile
        }
      }
      .get()
}
