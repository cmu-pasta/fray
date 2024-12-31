package org.pastalab.fray.idea.execute

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import org.pastalab.fray.idea.FrayConstants

class FrayGradleRunConfigurationType :
    AbstractExternalSystemTaskConfigurationType(FrayConstants.FRAY_GRADLE_ID) {
  override fun doCreateConfiguration(
      externalSystemId: ProjectSystemId,
      project: Project,
      factory: ConfigurationFactory,
      name: String
  ): ExternalSystemRunConfiguration {
    return FrayGradleRunConfiguration(project, factory, name)
  }

  override fun getConfigurationFactoryId(): String {
    return FrayConstants.FRAY_GRADLE_ID.readableName
  }

  companion object {
    fun getInstance() =
        ExternalSystemUtil.findConfigurationType(FrayConstants.FRAY_GRADLE_ID)
            as FrayGradleRunConfigurationType
  }
}
