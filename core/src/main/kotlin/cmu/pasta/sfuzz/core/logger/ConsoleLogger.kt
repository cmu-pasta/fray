package cmu.pasta.sfuzz.core.logger

import cmu.pasta.sfuzz.core.concurrency.operations.Operation
import cmu.pasta.sfuzz.core.runtime.AnalysisResult
import cmu.pasta.sfuzz.core.scheduler.Choice

class ConsoleLogger: LoggerBase {
    override fun executionStart() {
    }

    override fun newOperationScheduled(op: Operation, choice: Choice) {
        println("choice: $choice")
    }

    override fun executionDone(result: AnalysisResult) {
    }

    override fun applicationEvent(event: String) {
        println("event: $event")
    }
}