package org.pastalab.fray.junit.junit5

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import org.pastalab.fray.core.RunContext
import org.pastalab.fray.core.delegates.DelegateSynchronizer
import org.pastalab.fray.core.delegates.RuntimeDelegate
import org.pastalab.fray.runtime.Runtime

class FrayExtension(
    val index: Int,
    val frayContext: RunContext,
    val frayJupiterContext: FrayJupiterContext
) : AfterEachCallback, TestWatcher, ExecutionCondition, InvocationInterceptor, AfterAllCallback {

  override fun <T : Any?> interceptTestClassConstructor(
      invocation: InvocationInterceptor.Invocation<T>,
      invocationContext: ReflectiveInvocationContext<Constructor<T>>,
      extensionContext: ExtensionContext
  ): T {
    // The test class constructor is called even if the test is `disabled()`
    // So we need to skip the constructor interception if the test is disabled.
    if (frayJupiterContext.bugFound) return invocation.proceed()
    frayContext.config.currentIteration = index
    if (frayContext.config.currentIteration != 1) {
      frayContext.config.scheduler =
          frayContext.config.scheduler.nextIteration(
              frayContext.config.randomnessProvider.getRandomness())
      frayContext.config.randomness = frayContext.config.randomnessProvider.getRandomness()
    }
    val synchronizer = DelegateSynchronizer(frayContext)
    Runtime.NETWORK_DELEGATE =
        frayContext.config.networkDelegateType.produce(frayContext, synchronizer)
    Runtime.LOCK_DELEGATE = RuntimeDelegate(frayContext, synchronizer)
    Runtime.start()
    val result = invocation.proceed()
    Runtime.onSkipPrimitive("JUnit internal")
    return result
  }

  override fun interceptTestTemplateMethod(
      invocation: InvocationInterceptor.Invocation<Void?>?,
      invocationContext: ReflectiveInvocationContext<Method?>?,
      extensionContext: ExtensionContext?
  ) {
    Runtime.onSkipPrimitiveDone("JUnit internal")
    invocation?.proceed()
  }

  override fun afterEach(context: ExtensionContext) {
    if (context.executionException.isPresent) {
      handleError(context.executionException.get(), context.displayName)
    }
    try {
      Runtime.onMainExit()
    } finally {
      Runtime.resetAllDelegate()
    }
    if (frayContext.bugFound != null) {
      throw AssertionError(frayContext.bugFound!!)
    }
  }

  override fun testFailed(context: ExtensionContext, cause: Throwable) {
    handleError(cause, context.displayName)
  }

  private fun handleError(cause: Throwable, testName: String) {
    if (!frayJupiterContext.bugFound) {
      frayJupiterContext.bugFound = true
      frayContext.reportError(cause)
      println(
          "Bug found in iteration test ${testName}, you may find detailed report and replay files " +
              "in ${frayContext.config.report}")
    }
  }

  override fun evaluateExecutionCondition(context: ExtensionContext?): ConditionEvaluationResult {
    if (frayJupiterContext.bugFound) {
      return disabled("Bug found in previous iteration.")
    }
    return enabled("No bug found in previous iteration.")
  }

  override fun afterAll(context: ExtensionContext?) {
    frayContext.shutDown()
  }
}
