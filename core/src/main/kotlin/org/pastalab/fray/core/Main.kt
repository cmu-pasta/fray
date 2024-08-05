package org.pastalab.fray.core

import kotlin.system.exitProcess
import org.pastalab.fray.core.command.MainCommand

fun main(args: Array<String>) {
  val config = MainCommand().apply { main(args) }.toConfiguration()
  val runner = org.pastalab.fray.core.TestRunner(config)
  if (runner.run() != null) {
    exitProcess(-1)
  }
}
