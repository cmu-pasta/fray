package org.pastalab.fray.idea

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.JavaProgramPatcher

class FrayProgramPatcher: JavaProgramPatcher() {
  override fun patchJavaParameters(executor: Executor, profile: RunProfile, parameters: JavaParameters) {
    if (executor is FrayDebugExecutor) {
      parameters.vmParametersList.add("-Dfray.debugger=true")
    }
  }

}
