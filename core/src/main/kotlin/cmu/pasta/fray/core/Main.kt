package cmu.pasta.fray.core

import cmu.pasta.fray.core.command.MainCommand
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  val config = MainCommand().apply { main(args) }.toConfiguration()
  val runner = TestRunner(config)
  exitProcess(runner.run())
}
