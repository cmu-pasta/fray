package cmu.pasta.fray.core.logger

import cmu.pasta.fray.core.concurrency.operations.Operation
import cmu.pasta.fray.core.scheduler.Choice

class NoopLogger : LoggerBase {
  override fun executionStart() {}

  override fun newOperationScheduled(op: Operation, choice: Choice) {}

  override fun executionDone(shouldSave: Boolean) {}

  override fun applicationEvent(event: String) {}

  override fun shutdown() {}
}
