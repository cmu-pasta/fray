package org.anonlab.fray.core

import kotlin.system.exitProcess
import org.anonlab.fray.core.command.MainCommand

fun main(args: Array<String>) {
  val config = MainCommand().apply { main(args) }.toConfiguration()
  val runner = org.anonlab.fray.core.TestRunner(config)
  if (runner.run() != null) {
    exitProcess(-1)
  }
}
