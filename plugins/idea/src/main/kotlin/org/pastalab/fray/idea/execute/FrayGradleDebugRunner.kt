package org.pastalab.fray.idea.execute

import com.intellij.execution.configurations.RunProfile
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemTaskDebugRunner

class FrayGradleDebugRunner: ExternalSystemTaskDebugRunner() {
  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    return executorId == FrayDebugExecutor.EXECUTOR_ID && profile is FrayGradleRunConfiguration
  }
}
