package cmu.pasta.fray.core.logger

import cmu.pasta.fray.core.concurrency.operations.Operation
import cmu.pasta.fray.core.scheduler.Choice

interface LoggerBase {
  fun executionStart()

  fun newOperationScheduled(op: Operation, choice: Choice)

  fun executionDone(bugFound: Boolean)

  //  fun logError(error: String)

  fun applicationEvent(event: String)

  fun shutdown()
}
