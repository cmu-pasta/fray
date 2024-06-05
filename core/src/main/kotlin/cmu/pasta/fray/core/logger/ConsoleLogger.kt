package cmu.pasta.fray.core.logger

import cmu.pasta.fray.core.concurrency.operations.Operation
import cmu.pasta.fray.core.scheduler.Choice

class ConsoleLogger : LoggerBase {
  override fun executionStart() {}

  override fun newOperationScheduled(op: Operation, choice: Choice) {}

  override fun executionDone(bugFound: Boolean) {}

  override fun applicationEvent(event: String) {
    println(event)
  }

  override fun shutdown() {}
}
