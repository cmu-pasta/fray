package cmu.pasta.sfuzz.core.concurrency.logger

import cmu.pasta.sfuzz.core.concurrency.operations.Operation

interface LoggerBase {
    fun executionStart()
    fun newOperationScheduled(op: Operation)
    fun executionDone()
}