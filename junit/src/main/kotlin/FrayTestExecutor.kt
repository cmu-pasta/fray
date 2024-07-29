package cmu.edu.pasta.fray.junit

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
    val testMethod = descriptor.testMethod
    request.engineExecutionListener.executionFinished(descriptor, TestExecutionResult.successful())
  }

  fun executeContainer(request: ExecutionRequest, container: TestDescriptor) {
    request.engineExecutionListener.executionStarted(container)
    container.children.forEach {
      execute(request, it)
    }
    request.engineExecutionListener.executionFinished(container, TestExecutionResult.successful())
  }
}
