package cmu.pasta.sfuzz.core.logger

import cmu.pasta.sfuzz.core.concurrency.operations.Operation
import cmu.pasta.sfuzz.core.runtime.AnalysisResult
import cmu.pasta.sfuzz.core.scheduler.Choice
import java.io.File

class CsvLogger(val baseFolder: String): LoggerBase {
    var index = 0
    var scheduleFile: File? = null
    override fun executionStart() {
        scheduleFile = File("$baseFolder/schedule_$index.csv")
        scheduleFile?.appendText("selected,threadId,enabled,operation\n")
    }

    override fun newOperationScheduled(op: Operation, choice: Choice) {
        scheduleFile?.appendText("${choice.selected},${choice.threadId},${choice.enabled},${op.javaClass.name}\n")
    }

    override fun executionDone(result: AnalysisResult) {
        index += 1
    }

    override fun applicationEvent(event: String) {
    }
}