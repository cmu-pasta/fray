package org.pastalab.fray.idea

import com.intellij.execution.JavaTestFrameworkDebuggerRunner
import com.intellij.execution.configurations.RunProfile

class FrayJunitDebuggerRunner: JavaTestFrameworkDebuggerRunner() {
  override fun getRunnerId(): String {
    return "JUnitDebug"
  }

  override fun validForProfile(profile: RunProfile): Boolean {
    return profile.javaClass.name.contains("JUnitConfiguration")
  }

  override fun getThreadName(): String {
    return "junit"
  }

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    println(profile.name)
    return executorId == FrayDebugExecutor.EXECUTOR_ID && validForProfile(profile)
  }

}
