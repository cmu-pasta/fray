package cmu.edu.pasta.fray.junit

import cmu.pasta.fray.core.GlobalContext.loggers
import cmu.pasta.fray.core.TestRunner
import cmu.pasta.fray.core.command.Configuration
import cmu.pasta.fray.core.command.ExecutionInfo
import cmu.pasta.fray.core.command.LambdaExecutor
import cmu.pasta.fray.core.logger.JsonLogger
import java.util.*
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.support.descriptor.EngineDescriptor

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
    val config =
        Configuration(
            ExecutionInfo(
                LambdaExecutor { testMethod.invoke(testInstance) },
                false,
                true,
                false,
                -1,
            ),
            "fray-report",
            descriptor.analyzeConfig.iteration,
            descriptor.getScheduler(),
            true,
            JsonLogger("fray-report", false),
            false,
            true,
            false,
        )
    val logger = EventLogger()
    loggers.add(logger)
    val runner = TestRunner(config)
    val result = runner.run()
    verifyTestResult(request, descriptor, result, logger)
  }

  fun verifyTestResult(
      request: ExecutionRequest,
      descriptor: MethodTestDescriptor,
      result: Throwable?,
      logger: EventLogger
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
    if (descriptor.analyzeConfig.expectedLog != "" &&
        !logger.sb.toString().contains(descriptor.analyzeConfig.expectedLog)) {
      testResult =
          TestExecutionResult.failed(
              RuntimeException("Expected log not found: ${descriptor.analyzeConfig.expectedLog}"))
    }
    request.engineExecutionListener.executionFinished(descriptor, testResult)
  }

  fun executeContainer(request: ExecutionRequest, container: TestDescriptor) {
    request.engineExecutionListener.executionStarted(container)
    container.children.forEach { execute(request, it) }
    request.engineExecutionListener.executionFinished(container, TestExecutionResult.successful())
  }
}
