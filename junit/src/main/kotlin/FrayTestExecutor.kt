package org.pastalab.fray.junit

import java.io.File
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import org.pastalab.fray.core.TestRunner
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.command.ExecutionInfo
import org.pastalab.fray.core.command.LambdaExecutor

class FrayTestExecutor {
  fun execute(request: ExecutionRequest, descriptor: TestDescriptor) {
    if (descriptor is EngineDescriptor) {
      executeContainer(request, descriptor)
    }
    if (descriptor is ClassTestDescriptor) {
      executeContainer(request, descriptor)
    }
    if (descriptor is MethodTestDescriptor) {
      executeTest(request, descriptor)
    }
  }

  fun executeTest(request: ExecutionRequest, descriptor: MethodTestDescriptor) {
    request.engineExecutionListener.executionStarted(descriptor)
    val testInstance = descriptor.parent.testClass.getDeclaredConstructor().newInstance()
    val testMethod = descriptor.testMethod
    val workDir = createTempDirectory(WORK_DIR).absolutePathString()
    val schedulerInfo = descriptor.getScheduler()
    val config =
        Configuration(
            ExecutionInfo(
                LambdaExecutor { testMethod.invoke(testInstance) },
                false,
                true,
                false,
                -1,
            ),
            workDir,
            descriptor.analyzeConfig.iteration,
            schedulerInfo.first,
            schedulerInfo.second,
            false,
            false,
            true,
            false,
            false,
        )
    val runner = TestRunner(config)
    val result = runner.run()
    verifyTestResult(request, descriptor, result, workDir)
  }

  fun verifyTestResult(
      request: ExecutionRequest,
      descriptor: MethodTestDescriptor,
      result: Throwable?,
      workDir: String,
  ) {
    var testResult = TestExecutionResult.successful()
    if (result != null) {
      if (descriptor.analyzeConfig.expectedException.java != result.javaClass) {
        testResult = TestExecutionResult.failed(result)
      }
    } else {
      if (descriptor.analyzeConfig.expectedException != Any::class) {
        testResult =
            TestExecutionResult.failed(
                RuntimeException(
                    "Expected exception not thrown: ${descriptor.analyzeConfig.expectedException.simpleName}"))
      }
    }
    if (testResult == TestExecutionResult.successful()) {
      File(workDir).deleteRecursively()
    } else {
      println("Test: ${descriptor.uniqueId} failed: report can be found at: $workDir")
    }
    request.engineExecutionListener.executionFinished(descriptor, testResult)
  }

  fun executeContainer(request: ExecutionRequest, container: TestDescriptor) {
    request.engineExecutionListener.executionStarted(container)
    container.children.forEach { execute(request, it) }
    request.engineExecutionListener.executionFinished(container, TestExecutionResult.successful())
  }

  companion object {
    val WORK_DIR = Paths.get(System.getProperty("fray.workDir", "fray/fray-report"))

    init {
      WORK_DIR.toFile().mkdirs()
    }
  }
}
