package org.pastalab.fray.idea.execute

import com.intellij.execution.Executor
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon
import org.pastalab.fray.idea.FrayBundle

class FrayDebugExecutor : Executor() {
  override fun getToolWindowId(): String {
    return EXECUTOR_ID
  }

  override fun getToolWindowIcon(): Icon {
    return AllIcons.Toolwindows.ToolWindowDebugger
  }

  override fun getIcon(): Icon {
    return AllIcons.Actions.StartDebugger
  }

  override fun getDisabledIcon(): Icon {
    return IconLoader.getDisabledIcon(icon)
  }

  override fun getDescription(): String {
    return FrayBundle.INSTANCE.getMessage("fray.debugger.startActionText")
  }

  override fun getActionName(): String {
    return EXECUTOR_ID
  }

  override fun getStartActionText(): String {
    return FrayBundle.INSTANCE.getMessage("fray.debugger.startActionText")
  }

  override fun getId(): String {
    return EXECUTOR_ID
  }

  override fun getContextActionId(): String {
    return this.id + " context-action-does-not-exist"
  }

  override fun getHelpId(): String {
    return "debugging.DebugWindow"
  }

  companion object {
    const val EXECUTOR_ID = "Debug (Fray)"
  }
}
