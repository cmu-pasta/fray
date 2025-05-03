package org.pastalab.fray.idea.debugger

import com.intellij.openapi.project.Project
import org.pastalab.fray.rmi.TestStatusObserver

class FrayRemoteTestObserver(val project: Project) : TestStatusObserver {
  val observers = mutableListOf<TestStatusObserver>()

  override fun onExecutionStart() {
    observers.forEach { it.onExecutionStart() }
  }

  override fun onExecutionDone(bugFound: Throwable?) {
    observers.forEach { it.onExecutionDone(bugFound) }
  }

  override fun saveToReportFolder(path: String) {
    observers.forEach { it.saveToReportFolder(path) }
  }
}
