package org.pastalab.fray.idea.execute

import com.intellij.execution.Executor
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import org.pastalab.fray.idea.FrayConstants.FRAY_DEBUGGER_KEY
import org.pastalab.fray.rmi.Constant.FRAY_DEBUGGER_DEBUG
import org.pastalab.fray.rmi.Constant.FRAY_DEBUGGER_DISABLED
import org.pastalab.fray.rmi.Constant.FRAY_DEBUGGER_PROPERTY_KEY
import org.pastalab.fray.rmi.Constant.FRAY_DEBUGGER_REPLAY

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
    configuration.putUserData(FRAY_DEBUGGER_KEY, FRAY_DEBUGGER_DISABLED)
    when (configuration) {
      is FrayGradleRunConfiguration -> {
        configuration.settings.taskNames.removeIf { it.contains(FRAY_DEBUGGER_PROPERTY_KEY) }
      }
      else -> {
        params.vmParametersList.addProperty(FRAY_DEBUGGER_PROPERTY_KEY, FRAY_DEBUGGER_DISABLED)
      }
    }
    if (executor.id == FrayDebugExecutor.EXECUTOR_ID) {
      configuration.putUserData(FRAY_DEBUGGER_KEY, FRAY_DEBUGGER_DEBUG)
      when (configuration) {
        is FrayGradleRunConfiguration -> {
          configuration.settings.taskNames.add("-P$FRAY_DEBUGGER_PROPERTY_KEY=$FRAY_DEBUGGER_DEBUG")
        }
        else -> {
          params.vmParametersList.addProperty(FRAY_DEBUGGER_PROPERTY_KEY, FRAY_DEBUGGER_DEBUG)
        }
      }
    }
    if (executor.id == FrayReplayerExecutor.EXECUTOR_ID) {
      configuration.putUserData(FRAY_DEBUGGER_KEY, FRAY_DEBUGGER_REPLAY)
      when (configuration) {
        is FrayGradleRunConfiguration -> {
          configuration.settings.taskNames.add(
              "-P$FRAY_DEBUGGER_PROPERTY_KEY=$FRAY_DEBUGGER_REPLAY")
        }
        else -> {
          params.vmParametersList.addProperty(FRAY_DEBUGGER_PROPERTY_KEY, FRAY_DEBUGGER_REPLAY)
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
