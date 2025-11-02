package org.pastalab.fray.junit.junit5

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext

/**
 * A TestTemplateInvocationContext that marks the invocation as disabled (skipped) with the provided
 * reason. This allows tests to be discovered and reported as skipped instead of being silently
 * omitted.
 */
class DisabledFrayInvocationContext(
    private val index: Int,
    private val displayName: String,
    private val total: Int,
    private val reason: String
) : TestTemplateInvocationContext {

  override fun getDisplayName(invocationIndex: Int): String {
    return "$displayName repetition $index of $total (skipped)"
  }

  override fun getAdditionalExtensions(): MutableList<Extension> {
    return mutableListOf(AlwaysDisabledCondition(reason))
  }
}

private class AlwaysDisabledCondition(private val reason: String) : ExecutionCondition {
  override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
    return ConditionEvaluationResult.disabled(reason)
  }
}
