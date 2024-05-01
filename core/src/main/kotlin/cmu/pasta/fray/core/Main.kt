package cmu.pasta.fray.core

fun main(args: Array<String>) {
  val config = ConfigurationCommand().apply { main(args) }.toConfiguration()
  val runner = TestRunner(config)
  runner.run()
}