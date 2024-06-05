package cmu.pasta.fray.core.logger

import cmu.pasta.fray.core.concurrency.operations.Operation
import cmu.pasta.fray.core.scheduler.Choice
import cmu.pasta.fray.core.scheduler.Schedule
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Record(
    val timeline: MutableList<Operation>,
    val schedule: Schedule,
)

class JsonLogger(val base: String, val fullSchedule: Boolean) : LoggerBase {
  var currentTimeline = mutableListOf<Operation>()
  var schedule = Schedule(mutableListOf(), fullSchedule)
  var savedSchedule = 0
  val json = Json { prettyPrint = true }

  override fun executionStart() {
    currentTimeline = mutableListOf()
    schedule = Schedule(mutableListOf(), fullSchedule)
  }

  override fun newOperationScheduled(op: Operation, choice: Choice) {
    currentTimeline.add(op)
    schedule.choices.add(choice)
  }

  override fun executionDone(bugFound: Boolean) {
    //    executions.add(Record(currentTimeline, schedule))
    //        if (result != AnalysisResult.COMPLETE) {
    //
    // File("$base/schedule_${savedSchedule++}.json").writeText(json.encodeToString(schedule))
    //        }
    if (bugFound) {
      File("$base/schedule_${savedSchedule++}.json").writeText(json.encodeToString(schedule))
    }
  }

  override fun applicationEvent(event: String) {}

  override fun shutdown() {
    //    File("$base/timeline.json").writeText(json.encodeToString(executions))
  }
}
