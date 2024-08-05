package org.pastalab.fray.core.logger

import org.pastalab.fray.core.concurrency.operations.Operation
import org.pastalab.fray.core.scheduler.Choice

class NoopLogger : LoggerBase {
  override fun executionStart() {}

  override fun newOperationScheduled(op: Operation, choice: Choice) {}

  override fun executionDone(shouldSave: Boolean) {}

  override fun applicationEvent(event: String) {}

  override fun shutdown() {}
}
