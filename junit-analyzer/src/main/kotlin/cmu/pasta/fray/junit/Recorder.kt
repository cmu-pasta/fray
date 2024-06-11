package cmu.pasta.fray.junit

import cmu.pasta.fray.runtime.Runtime
import junit.framework.TestCase
import java.io.File
import java.lang.instrument.Instrumentation

object Recorder {
  var newThreadSpawned = false
  val f = File("/tmp/junit.log")
  fun logThread() {
    newThreadSpawned = true
  }

  @JvmStatic
  fun testStart(testCase: TestCase) {
    newThreadSpawned = false
  }

  @JvmStatic
  fun testEnd(testCase: TestCase) {
    if (newThreadSpawned) {
      f.appendText("${testCase.javaClass.name}#${testCase.name}\n")
    }
    newThreadSpawned = false
  }

  fun init() {
    f.deleteOnExit()
    f.createNewFile()
  }
}


fun premain(arguments: String?, instrumentation: Instrumentation) {
  Recorder.init()
  Runtime.DELEGATE = JunitRuntimeDelegate()
  instrumentation.addTransformer(JunitRunnerTransformer())
}
