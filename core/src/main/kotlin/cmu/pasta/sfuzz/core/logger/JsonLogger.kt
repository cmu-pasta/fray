package cmu.pasta.sfuzz.core.logger

import cmu.pasta.sfuzz.core.concurrency.operations.Operation
import cmu.pasta.sfuzz.core.runtime.AnalysisResult
import cmu.pasta.sfuzz.core.scheduler.Choice
import cmu.pasta.sfuzz.core.scheduler.Schedule
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Record(val timeline: MutableList<Operation>, val schedule: Schedule, val result: AnalysisResult)

class JsonLogger(val base: String, val fullSchedule: Boolean): LoggerBase {
    val executions = mutableListOf<Record>()
    var currentTimeline = mutableListOf<Operation>()
    var schedule = Schedule(mutableListOf(), fullSchedule)
    var savedSchedule = 0
    val json = Json {
        prettyPrint = true
    }
    override fun executionStart() {
        currentTimeline = mutableListOf()
        schedule = Schedule(mutableListOf(), fullSchedule)
    }
    override fun newOperationScheduled(op: Operation, choice: Choice) {
        currentTimeline.add(op)
        schedule.choices.add(choice)
    }
    override fun executionDone(result: AnalysisResult) {
        executions.add(Record(currentTimeline, schedule, result))
//        if (result != AnalysisResult.COMPLETE) {
//            File("$base/schedule_${savedSchedule++}.json").writeText(json.encodeToString(schedule))
//        }
        File("$base/schedule_${savedSchedule++}.json").writeText(json.encodeToString(schedule))
    }
    override fun applicationEvent(event: String) {
    }
    fun dump() {
//        File("$base/timeline.json").writeText(json.encodeToString(executions))
    }
}