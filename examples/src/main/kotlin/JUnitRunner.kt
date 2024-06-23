package cmu.pasta.fray.examples

import org.junit.runner.JUnitCore
import org.junit.runner.Request
import org.junit.runner.manipulation.Filter

fun main(args: Array<String>) {
  val classAndMethod = args[0].split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
  val request =
      Request.method(
              Class.forName(classAndMethod[0], true, Thread.currentThread().contextClassLoader),
              classAndMethod[1],
          )
          .filterWith(
              object : Filter() {
                var found = false

                override fun shouldRun(description: org.junit.runner.Description): Boolean {
                  if (found) return false
                  if (description.methodName == classAndMethod[1]) {
                    found = true
                  }
                  return found
                }

                override fun describe(): String {
                  return classAndMethod[1]
                }
              })

  val result = JUnitCore().run(request)
  if (!result.wasSuccessful()) {
    val failureReport =
        result.failures.joinToString {
          "testHeader: ${it.testHeader}\n" +
              "trace: ${it.trace}\n" +
              "description: ${it.description}\n"
        }
    throw RuntimeException(failureReport)
  }
}
