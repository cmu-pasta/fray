package org.pastalab.fray.core.logger

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.pastalab.fray.core.concurrency.operations.Operation
import org.pastalab.fray.core.scheduler.Choice
import org.pastalab.fray.core.scheduler.Schedule

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

  override fun executionDone(shouldSave: Boolean) {
    //    executions.add(Record(currentTimeline, schedule))
    //        if (result != AnalysisResult.COMPLETE) {
    //
    // File("$base/schedule_${savedSchedule++}.json").writeText(json.encodeToString(schedule))
    //        }
    if (shouldSave) {
      saveLogs()
    }
  }

  fun saveLogs() {
    File("$base/schedule_${savedSchedule++}.json").writeText(json.encodeToString(schedule))
  }

  override fun applicationEvent(event: String) {}

  override fun shutdown() {
    //    File("$base/timeline.json").writeText(json.encodeToString(executions))
  }
}
