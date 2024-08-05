package org.pastalab.fray.junit

import org.pastalab.fray.core.concurrency.operations.Operation
import org.pastalab.fray.core.logger.LoggerBase
import org.pastalab.fray.core.scheduler.Choice

class EventLogger : LoggerBase {
  var sb: StringBuilder = StringBuilder()

  override fun executionStart() {}

  override fun newOperationScheduled(op: Operation, choice: Choice) {}

  override fun executionDone(shouldSave: Boolean) {}

  override fun applicationEvent(event: String) {
    sb.append(event)
  }

  override fun shutdown() {}
}
