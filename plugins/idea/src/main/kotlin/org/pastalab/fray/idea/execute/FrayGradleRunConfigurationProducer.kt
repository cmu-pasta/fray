package org.pastalab.fray.idea.execute

import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemRunConfigurationProducer
import org.jetbrains.plugins.gradle.execution.test.runner.TestMethodGradleConfigurationProducer
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

class FrayGradleRunConfigurationProducer: TestMethodGradleConfigurationProducer() {

  override fun isPreferredConfiguration(
    self: ConfigurationFromContext,
    other: ConfigurationFromContext
  ): Boolean {
    return true
  }

  override fun shouldReplace(
    self: ConfigurationFromContext,
    other: ConfigurationFromContext
  ): Boolean {
    return true
  }

  override fun getConfigurationFactory(): ConfigurationFactory {
    return FrayGradleRunConfigurationType.getInstance().factory
  }
}
