package org.pastalab.fray.core.logger

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FrayLogger(location: String) {
  val logFile = File(location)

  @Synchronized
  fun log(level: String, message: String) {
    val currentDateTime =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    val logMessage = "$currentDateTime [$level]: $message\n"
    logFile.appendText(logMessage)
  }

  fun info(message: String) {
    log("INFO", message)
  }

  fun error(message: String) {
    log("ERROR", message)
  }
}
