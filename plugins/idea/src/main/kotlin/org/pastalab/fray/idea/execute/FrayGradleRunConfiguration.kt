package org.pastalab.fray.idea.execute

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunnableState
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

class FrayGradleRunConfiguration(project: Project, factory: ConfigurationFactory, name: String): GradleRunConfiguration(project, factory, name) {
  override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
    // Allow debugging for Fray
    val isStateForDebug = ToolWindowId.DEBUG == executor.id || executor.id == FrayDebugExecutor.EXECUTOR_ID
    val runnableState = ExternalSystemRunnableState(
        settings,
        project, isStateForDebug, this, env,
    )
    copyUserDataTo(runnableState)
    return runnableState
  }
}
