package org.pastalab.fray.idea.debugger

import com.intellij.openapi.project.Project
import java.nio.file.Path
import org.pastalab.fray.rmi.TestStatusObserver

class FrayRemoteTestObserver(val project: Project) : TestStatusObserver {
  val observers = mutableListOf<TestStatusObserver>()

  override fun onExecutionStart() {
    observers.forEach { it.onExecutionStart() }
  }

  override fun onReportError(throwable: Throwable) {
    observers.forEach { it.onReportError(throwable) }
  }

  override fun onExecutionDone(bugFound: Throwable?) {
    observers.forEach { it.onExecutionDone(bugFound) }
  }

  override fun saveToReportFolder(path: Path) {
    observers.forEach { it.saveToReportFolder(path) }
  }
}
