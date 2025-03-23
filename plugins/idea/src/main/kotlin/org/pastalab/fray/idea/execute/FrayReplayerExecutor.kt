package org.pastalab.fray.idea.execute

import com.intellij.icons.AllIcons
import javax.swing.Icon
import org.pastalab.fray.idea.FrayBundle

class FrayReplayerExecutor : FrayExecutor() {

  override fun getToolWindowIcon(): Icon {
    return AllIcons.Toolwindows.ToolWindowDebugger
  }

  override fun getIcon(): Icon {
    return AllIcons.Actions.RestartDebugger
  }

  override fun getDescription(): String {
    return FrayBundle.INSTANCE.getMessage("fray.replayer.startActionText")
  }

  override fun getStartActionText(): String {
    return FrayBundle.INSTANCE.getMessage("fray.replayer.startActionText")
  }

  override fun getId(): String {
    return EXECUTOR_ID
  }

  override fun getContextActionId(): String {
    return this.id + " context-action-does-not-exist"
  }

  companion object {
    const val EXECUTOR_ID = "Replayer (Fray)"
  }
}
