package org.pastalab.fray.idea.execute

import com.intellij.debugger.impl.GenericDebuggerRunner
import com.intellij.execution.configurations.ModuleRunProfile
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfile

class FrayGenericRunner : GenericDebuggerRunner() {
  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return (executorId == FrayDebugExecutor.EXECUTOR_ID ||
        executorId == FrayReplayerExecutor.EXECUTOR_ID) &&
        profile is ModuleRunProfile &&
        profile !is RunConfigurationWithSuppressedDefaultDebugAction
  }
}
