package org.pastalab.fray.core.observers

import com.antithesis.sdk.Assert
import org.pastalab.fray.rmi.TestStatusObserver

class AntithesisErrorReporter : TestStatusObserver {
  override fun onReportError(throwable: Throwable) {
    Assert.unreachable(
        "An error occurred during the test execution: ${throwable.message}, stack trace: ${throwable.stackTraceToString()}",
        null)
  }
}
