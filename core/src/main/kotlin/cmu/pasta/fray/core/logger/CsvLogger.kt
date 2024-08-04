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
    scheduleFile?.appendText("selected,threadId,enabled\n")
  }

  override fun newOperationScheduled(op: Operation, choice: Choice) {
    if (choice.enabled > 1 || fullSchedule) {
      scheduleFile?.appendText(
          "${choice.selected},${choice.threadId},${choice.enabled},${choice.enabledIds.joinToString(",")},${choice.operation}\n")
    }
  }

  override fun executionDone(shouldSave: Boolean) {
    index += 1
  }

  override fun applicationEvent(event: String) {}

  override fun shutdown() {}
}
