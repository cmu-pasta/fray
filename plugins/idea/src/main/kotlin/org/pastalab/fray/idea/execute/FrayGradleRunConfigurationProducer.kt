package org.pastalab.fray.idea.execute

import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.configurations.ConfigurationFactory
import org.jetbrains.plugins.gradle.execution.test.runner.TestMethodGradleConfigurationProducer

class FrayGradleRunConfigurationProducer : TestMethodGradleConfigurationProducer() {

  override fun isPreferredConfiguration(
      self: ConfigurationFromContext,
      other: ConfigurationFromContext,
  ): Boolean {
    return super.isPreferredConfiguration(self, other)
  }

  override fun shouldReplace(
      self: ConfigurationFromContext,
      other: ConfigurationFromContext,
  ): Boolean {
    return super.shouldReplace(self, other)
  }

  override fun getConfigurationFactory(): ConfigurationFactory {
    return FrayGradleRunConfigurationType.getInstance().factory
  }
}
