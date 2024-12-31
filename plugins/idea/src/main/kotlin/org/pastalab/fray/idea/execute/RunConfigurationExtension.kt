package org.pastalab.fray.idea.execute

import com.intellij.execution.Executor
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings

class RunConfigurationExtension: RunConfigurationExtension() {
  override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
    return configuration.type is FrayGradleRunConfigurationType || listOf("Java", "Kotlin", "Application", "JAR Application")
        .any { configuration.type.displayName.startsWith(it) }
  }

  override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
    configuration: T & Any,
    params: JavaParameters,
    runnerSettings: RunnerSettings?,
    executor: Executor,
  ) {
    if (executor.id == FrayDebugExecutor.EXECUTOR_ID) {
      when (configuration) {
        is FrayGradleRunConfiguration -> {
          if ("-Pfray.debugger=true" !in configuration.settings.taskNames) {
            configuration.settings.taskNames.add("-Pfray.debugger=true")
          }
        }
        else -> {
          params.vmParametersList.add("-Dfray.debugger=true")
        }
      }
    }
  }

  override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
    configuration: T & Any,
    params: JavaParameters,
    runnerSettings: RunnerSettings?,
  ) {
  }
}
