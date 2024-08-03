package cmu.edu.pasta.fray.junit

import cmu.pasta.fray.core.concurrency.operations.Operation
import cmu.pasta.fray.core.logger.LoggerBase
import cmu.pasta.fray.core.scheduler.Choice

class EventLogger : LoggerBase {
  var sb: StringBuilder = StringBuilder()

  override fun executionStart() {}

  override fun newOperationScheduled(op: Operation, choice: Choice) {}

  override fun executionDone(bugFound: Boolean) {}

  override fun applicationEvent(event: String) {
    sb.append(event)
  }

  override fun shutdown() {}
}
