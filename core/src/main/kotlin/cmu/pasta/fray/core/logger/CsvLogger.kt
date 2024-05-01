package cmu.pasta.fray.core.logger

import cmu.pasta.fray.core.concurrency.operations.Operation
import cmu.pasta.fray.core.scheduler.Choice
import java.io.File

class CsvLogger(private val baseFolder: String, private val fullSchedule: Boolean) : LoggerBase {
  var index = 0
  var scheduleFile: File? = null

  override fun executionStart() {
    val type = if (fullSchedule) "full" else "simplified"
    scheduleFile = File("$baseFolder/schedule_${type}_$index.csv")
    scheduleFile?.appendText("selected,threadId,enabled,operation\n")
  }

  override fun newOperationScheduled(op: Operation, choice: Choice) {
    scheduleFile?.appendText(
        "${choice.selected},${choice.threadId},${choice.enabled},${op.javaClass.name}\n")
  }

  override fun executionDone() {
    index += 1
  }

  override fun applicationEvent(event: String) {}

  override fun shutdown() {}
}