package cmu.pasta.sfuzz.core.scheduler

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Schedule(val choices: MutableList<Choice>, val fullSchedule: Boolean) {
  companion object {
    fun fromString(schedule: String, isJson: Boolean, fullSchedule: Boolean): Schedule {
      if (isJson) {
        return Json.decodeFromString<Schedule>(schedule)
      }
      val choices = mutableListOf<Choice>()
      var skipFirstLine = false
      for (line in schedule.split("\n")) {
        if (!skipFirstLine) {
          skipFirstLine = true
          continue
        }
        if (line.strip().isEmpty()) continue
        val parts = line.split(",")
        choices.add(Choice(parts[0].toInt(), parts[1].toInt(), parts[2].toInt()))
      }
      return Schedule(choices, fullSchedule)
    }
  }
}
