package cmu.pasta.fray.examples

import org.junit.runner.JUnitCore
import org.junit.runner.Request

fun main(args: Array<String>) {
  val classAndMethod = args[0].split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
  val request =
      Request.method(
          Class.forName(classAndMethod[0], true, Thread.currentThread().contextClassLoader),
          classAndMethod[1],
      )

  val result = JUnitCore().run(request)
  if (!result.wasSuccessful()) {
    val failureReport = result.failures.joinToString("\n =========== \n")
    throw RuntimeException(failureReport)
  }
}
