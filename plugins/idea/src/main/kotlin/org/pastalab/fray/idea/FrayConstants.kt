package org.pastalab.fray.idea

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.util.Key
import org.pastalab.fray.rmi.Constant.FRAY_DEBUGGER_PROPERTY_KEY

object FrayConstants {
  val FRAY_GRADLE_ID = ProjectSystemId("fray-gradle", "Fray (Gradle)")
  val FRAY_DEBUGGER_KEY = Key.create<String>(FRAY_DEBUGGER_PROPERTY_KEY)
}
