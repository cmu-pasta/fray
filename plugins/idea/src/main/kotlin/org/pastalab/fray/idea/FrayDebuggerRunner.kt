package org.pastalab.fray.idea

import com.intellij.debugger.impl.GenericDebuggerRunner
import com.intellij.execution.configurations.ModuleRunProfile
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfile

class FrayDebuggerRunner: GenericDebuggerRunner() {
  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return executorId == FrayDebugExecutor.EXECUTOR_ID && profile is ModuleRunProfile && (profile !is RunConfigurationWithSuppressedDefaultDebugAction)
  }
}
