package cmu.pasta.sfuzz.core.logger

import cmu.pasta.sfuzz.core.concurrency.operations.Operation
import cmu.pasta.sfuzz.core.runtime.AnalysisResult
import cmu.pasta.sfuzz.core.scheduler.Choice
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Record(val timeline: MutableList<Operation>, val choices: List<Choice>, val result: AnalysisResult)

class JsonLogger(val base: String, val saveAllSchedules: Boolean): LoggerBase {
    val executions = mutableListOf<Record>()
    var currentTimeline = mutableListOf<Operation>()
    var choices = mutableListOf<Choice>()
    var savedSchedule = 0
    val json = Json {
        prettyPrint = true
    }
    override fun executionStart() {
        currentTimeline = mutableListOf()
        choices = mutableListOf()
    }
    override fun newOperationScheduled(op: Operation, choice: Choice) {
        currentTimeline.add(op)
        choices.add(choice)
    }
    override fun executionDone(result: AnalysisResult) {
        executions.add(Record(currentTimeline, choices, result))
        if (result != AnalysisResult.COMPLETE || saveAllSchedules) {
            File("$base/schedule_${savedSchedule++}.json").writeText(json.encodeToString(choices))
        }
    }

    fun dump() {
        File("$base/timeline.json").writeText(json.encodeToString(executions))
    }
}