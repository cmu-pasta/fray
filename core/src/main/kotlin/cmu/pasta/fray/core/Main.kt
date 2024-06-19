package cmu.pasta.fray.core

import cmu.pasta.fray.core.command.MainCommand

fun main(args: Array<String>) {
  val config = MainCommand().apply { main(args) }.toConfiguration()
  val runner = TestRunner(config)
  runner.run()
}
