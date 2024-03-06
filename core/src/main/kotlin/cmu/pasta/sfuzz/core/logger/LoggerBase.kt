package cmu.pasta.sfuzz.core.logger

import cmu.pasta.sfuzz.core.concurrency.operations.Operation
import cmu.pasta.sfuzz.core.runtime.AnalysisResult
import cmu.pasta.sfuzz.core.scheduler.Choice

interface LoggerBase {
    fun executionStart()
    fun newOperationScheduled(op: Operation, choice: Choice)
    fun executionDone(result: AnalysisResult)
    fun applicationEvent(event: String)
}