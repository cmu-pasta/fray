package org.pastalab.fray.idea.execute

import com.intellij.execution.Executor
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings

class RunConfigurationExtension : RunConfigurationExtension() {
  override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
    return configuration.type is FrayGradleRunConfigurationType ||
        listOf("Java", "Kotlin", "Application", "JAR Application").any {
          configuration.type.displayName.startsWith(it)
        }
  }

  override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
      configuration: T & Any,
      params: JavaParameters,
      runnerSettings: RunnerSettings?,
      executor: Executor,
  ) {
    val frayPropertyKey = "fray.debugger"
    val frayGradleString = "-P${frayPropertyKey}=true"
    if (executor.id == FrayDebugExecutor.EXECUTOR_ID) {
      when (configuration) {
        is FrayGradleRunConfiguration -> {
          if (frayGradleString !in configuration.settings.taskNames) {
            configuration.settings.taskNames.add(frayGradleString)
          }
        }
        else -> {
          params.vmParametersList.addProperty(frayPropertyKey, "true")
        }
      }
    } else {
      when (configuration) {
        is FrayGradleRunConfiguration -> {
          configuration.settings.taskNames.remove(frayGradleString)
        }
        else -> {
          params.vmParametersList.addProperty(frayPropertyKey, "false")
        }
      }
    }
  }

  override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
      configuration: T & Any,
      params: JavaParameters,
      runnerSettings: RunnerSettings?,
  ) {}
}
