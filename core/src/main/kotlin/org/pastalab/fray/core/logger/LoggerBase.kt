package org.pastalab.fray.core.logger

import org.pastalab.fray.core.concurrency.operations.Operation
import org.pastalab.fray.core.scheduler.Choice

interface LoggerBase {
  fun executionStart()

  fun newOperationScheduled(op: Operation, choice: Choice)

  fun executionDone(shouldSave: Boolean)

  //  fun logError(error: String)

  fun applicationEvent(event: String)

  fun shutdown()
}
