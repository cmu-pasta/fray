package cmu.pasta.fray.examples

import java.io.PrintWriter
import java.io.StringWriter
import org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.runner.JUnitCore
import org.junit.runner.Request
import org.junit.runner.manipulation.Filter

fun main(args: Array<String>) {
  val isJunit4 = args[0] == "junit4"
  val classAndMethod = args[1].split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

  if (isJunit4) {
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
                },
            )
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
  } else {
    val request =
        LauncherDiscoveryRequestBuilder.request()
            .selectors(selectMethod(classAndMethod[0], classAndMethod[1]))
            .build()
    val launcher = LauncherFactory.create()
    val listener = SummaryGeneratingListener()
    launcher.registerTestExecutionListeners(listener)
    launcher.execute(request)
    val summary = listener.summary
    if (summary.testsFailedCount > 0) {
      val failureReport =
          summary.failures.joinToString {
            val stringWriter = StringWriter()
            val writer = PrintWriter(stringWriter)
            it.exception.printStackTrace(writer)
            "testHeader: ${it.testIdentifier}\n" +
                "trace: $stringWriter\n" +
                "exception: ${it.exception}\n"
          }
      throw RuntimeException(failureReport)
    }
  }
}
