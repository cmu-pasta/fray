package cmu.pasta.fray.junit

import cmu.pasta.fray.runtime.Runtime
import java.io.File
import java.lang.instrument.Instrumentation
import junit.framework.TestCase
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.vintage.engine.descriptor.VintageTestDescriptor

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
  fun executionStarted(descriptor: TestDescriptor) {
    newThreadSpawned = false
  }

  @JvmStatic
  fun executionFinished(descriptor: TestDescriptor, result: TestExecutionResult) {
    if (newThreadSpawned) {
      if (descriptor is VintageTestDescriptor) {
        val methodName = descriptor.displayName
        if (descriptor.parent.isPresent) {
          val parent = descriptor.parent.get()
          if (parent.source.isPresent) {
            val source = parent.source.get()
            if (source is ClassSource) {
              source.className
              f.appendText("${source.className}#${methodName}\n")
            }
          }
        }
      }
    }
    newThreadSpawned = false
  }

  @JvmStatic
  fun execution(descriptor: TestDescriptor) {
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
    f.delete()
    f.createNewFile()
  }
}

fun premain(arguments: String?, instrumentation: Instrumentation) {
  Recorder.init()
  Runtime.DELEGATE = JunitRuntimeDelegate()
  instrumentation.addTransformer(JunitRunnerTransformer())
}
