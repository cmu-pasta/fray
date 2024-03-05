package cmu.pasta.sfuzz.core.logger

import cmu.pasta.sfuzz.core.concurrency.operations.Operation
import cmu.pasta.sfuzz.core.runtime.AnalysisResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Record(val timeline: MutableList<Operation>, val choices: List<Pair<Int, Int>>, val result: AnalysisResult)

class JsonLogger(val base: String): LoggerBase {
    val executions = mutableListOf<Record>()

    var currentTimeline = mutableListOf<Operation>()
    var choices = mutableListOf<Pair<Int, Int>>()

    val json = Json {
        prettyPrint = true
    }
    override fun executionStart() {
        currentTimeline = mutableListOf()
        choices = mutableListOf()
    }
    override fun newOperationScheduled(op: Operation, choice: Pair<Int, Int>) {
        currentTimeline.add(op)
        choices.add(choice)
    }
    override fun executionDone(result: AnalysisResult) {
        executions.add(Record(currentTimeline, choices, result))
    }
    fun dump() {
        File("$base/timeline.json").writeText(json.encodeToString(executions))
    }
}