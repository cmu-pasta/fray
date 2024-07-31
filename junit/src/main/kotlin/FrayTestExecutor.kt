package cmu.edu.pasta.fray.junit

import cmu.pasta.fray.core.TestRunner
import cmu.pasta.fray.core.command.Configuration
import cmu.pasta.fray.core.command.ExecutionInfo
import cmu.pasta.fray.core.command.LambdaExecutor
import cmu.pasta.fray.core.logger.JsonLogger
import cmu.pasta.fray.core.scheduler.POSScheduler
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import java.util.*

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
    val config = Configuration(
        ExecutionInfo(
            LambdaExecutor {
              testMethod.invoke(testInstance)
            },
            false,
            true,
            false,
            -1
        ),
        "fray-report",
        10000,
        POSScheduler(Random()),
        true,
        JsonLogger("fray-report", false),
        false,
        true,
        false
    )
    val runner = TestRunner(config)
    val result = runner.run()
    if (result != null) {
      request.engineExecutionListener.executionFinished(descriptor, TestExecutionResult.failed(result))
    } else {
      request.engineExecutionListener.executionFinished(descriptor, TestExecutionResult.successful())
    }
  }

  fun executeContainer(request: ExecutionRequest, container: TestDescriptor) {
    request.engineExecutionListener.executionStarted(container)
    container.children.forEach {
      execute(request, it)
    }
    request.engineExecutionListener.executionFinished(container, TestExecutionResult.successful())
  }
}
