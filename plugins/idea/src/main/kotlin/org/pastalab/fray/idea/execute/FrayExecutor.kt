package org.pastalab.fray.idea.execute

import com.intellij.execution.Executor
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

abstract class FrayExecutor : Executor() {
  override fun getToolWindowId(): String {
    return id
  }

  override fun getDisabledIcon(): Icon {
    return IconLoader.getDisabledIcon(icon)
  }

  override fun getActionName(): String {
    return id
  }

  override fun getContextActionId(): String {
    return this.id + " context-action-does-not-exist"
  }

  override fun getHelpId(): String {
    return "debugging.DebugWindow"
  }
}
