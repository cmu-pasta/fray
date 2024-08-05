package org.pastalab.fray.core.logger

import java.io.File
import org.pastalab.fray.core.concurrency.operations.Operation
import org.pastalab.fray.core.scheduler.Choice

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
