package org.pastalab.fray.junit.junit5

import java.lang.reflect.Constructor
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import org.pastalab.fray.core.RunContext
import org.pastalab.fray.core.RuntimeDelegate
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.runtime.Delegate
import org.pastalab.fray.runtime.Runtime

class FrayExtension(
    val index: Int,
    val frayContext: RunContext,
    val frayJupiterContext: FrayJupiterContext
) :
    BeforeEachCallback,
    AfterEachCallback,
    TestWatcher,
    ExecutionCondition,
    InvocationInterceptor,
    AfterAllCallback {

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
      frayContext.config.scheduler = frayContext.config.scheduler.nextIteration()
      frayContext.config.randomnessProvider = ControlledRandom()
    }
    Runtime.DELEGATE = RuntimeDelegate(frayContext)
    Runtime.start()
    val result = invocation.proceed()
    Runtime.onSkipMethod("JUnit internal")
    return result
  }

  override fun beforeEach(context: ExtensionContext?) {
    Runtime.onSkipMethodDone("JUnit internal")
  }

  override fun afterEach(context: ExtensionContext) {
    try {
      Runtime.onMainExit()
    } finally {
      Runtime.DELEGATE = Delegate()
    }
    if (frayContext.bugFound != null) {
      throw AssertionError(frayContext.bugFound!!)
    }
  }

  override fun testFailed(context: ExtensionContext, cause: Throwable) {
    frayJupiterContext.bugFound = true
    frayContext.reportError(cause)
    println(
        "Bug found in iteration test ${context.displayName}, you may find detailed report and replay files " +
            "in ${frayContext.config.report}")
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
