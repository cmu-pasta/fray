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
          configuration.settings.env.getOrDefault("GRADLE_OPTS", "").let {
            configuration.settings.env["GRADLE_OPTS"] = "$it -Dfray.debugger=true"
          }
//          params.env.getOrDefault("GRADLE_OPTS", "").let {
//            params.env["GRADLE_OPTS"] = "$it -Dfray.debugger=true"
//          }
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
