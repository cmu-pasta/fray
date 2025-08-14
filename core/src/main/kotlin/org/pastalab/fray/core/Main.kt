package org.pastalab.fray.core

import com.github.ajalt.clikt.core.main
import kotlin.system.exitProcess
import org.pastalab.fray.core.command.MainCommand

fun main(args: Array<String>) {
  val config = MainCommand().apply { main(args) }.toConfiguration()
  val runner = TestRunner(config)
  if (runner.run() != null) {
    exitProcess(-1)
  }
}
