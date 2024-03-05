package cmu.pasta.sfuzz.core.logger

import cmu.pasta.sfuzz.core.concurrency.operations.Operation
import cmu.pasta.sfuzz.core.runtime.AnalysisResult

interface LoggerBase {
    fun executionStart()
    fun newOperationScheduled(op: Operation, choice: Pair<Int, Int>);
    fun executionDone(result: AnalysisResult)
}