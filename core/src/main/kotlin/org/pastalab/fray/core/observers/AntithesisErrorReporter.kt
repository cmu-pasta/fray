package org.pastalab.fray.core.observers

import com.antithesis.sdk.Assert
import com.fasterxml.jackson.databind.ObjectMapper
import org.pastalab.fray.rmi.TestStatusObserver

class AntithesisErrorReporter : TestStatusObserver {
  override fun onExecutionStart() {
    // No-op for Antithesis error reporter
  }

  override fun onReportError(throwable: Throwable) {
    val objectMapper = ObjectMapper()
    val node = objectMapper.createObjectNode()
    node.put("message", throwable.message)
    node.put("stackTrace", throwable.stackTraceToString())
    Assert.unreachable("An error occurred during the Fray test", node)
  }

  override fun onExecutionDone(bugFound: Throwable?) {
    // No-op for Antithesis error reporter
  }

  override fun saveToReportFolder(path: String) {
    // No-op for Antithesis error reporter
  }
}
