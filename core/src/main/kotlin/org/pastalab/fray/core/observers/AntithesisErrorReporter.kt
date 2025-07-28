package org.pastalab.fray.core.observers

import com.antithesis.sdk.Assert
import org.pastalab.fray.rmi.TestStatusObserver

class AntithesisErrorReporter : TestStatusObserver {
  override fun onExecutionStart() {}

  override fun onExecutionDone(bugFound: Throwable?) {
    if (bugFound != null) {
      Assert.unreachable(
          "An error occurred during the test execution: ${bugFound.message}, stack trace: ${bugFound.stackTraceToString()}",
          null)
    }
  }

  override fun saveToReportFolder(path: String) {}
}
