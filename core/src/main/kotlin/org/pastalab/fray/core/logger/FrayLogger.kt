package org.pastalab.fray.core.logger

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FrayLogger(location: String) {
  val logFile = File(location)
  val stdout = System.out

  @Synchronized
  fun log(level: String, message: String, printToStdIO: Boolean) {
    val currentDateTime =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    val logMessage = "$currentDateTime [$level]: $message\n"
    logFile.appendText(logMessage)
    if (printToStdIO) {
      stdout.println(logMessage)
    }
  }

  fun info(message: String, printToStdIO: Boolean = false) {
    log("INFO", message, printToStdIO)
  }

  fun error(message: String, printToStdIO: Boolean = false) {
    log("ERROR", message, printToStdIO)
  }
}
